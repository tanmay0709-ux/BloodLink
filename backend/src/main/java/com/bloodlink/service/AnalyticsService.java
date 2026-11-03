package com.bloodlink.service;

import com.bloodlink.dto.DashboardAnalyticsDto;
import com.bloodlink.dto.DashboardAnalyticsDto.*;
import com.bloodlink.model.*;
import com.bloodlink.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final BloodUnitRepository bloodUnitRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonationRepository donationRepository;
    private final DonorRepository donorRepository;
    private final BloodDispatchRepository bloodDispatchRepository;
    private final BloodAllocationRepository bloodAllocationRepository;
    private final BloodUnitService bloodUnitService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("User not found: " + auth.getName()));
    }

    @Transactional
    public DashboardAnalyticsDto getDashboardAnalytics() {
        // Ensure stale unexpired units are automatically marked EXPIRED before computing stats
        bloodUnitService.checkAndMarkExpiredUnits();

        User currentUser = getCurrentUser();
        User.Role role = currentUser.getRole();

        Long targetHospitalId = null;
        Long targetBloodBankId = null;
        String facilityName = "Global BloodLink Network";
        String facilityType = "ALL";
        Long facilityId = null;

        if (role == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() != null) {
                targetHospitalId = currentUser.getAssignedHospital().getId();
                facilityName = currentUser.getAssignedHospital().getName();
                facilityId = targetHospitalId;
            } else {
                facilityName = "Unassigned Hospital Staff";
                facilityId = null;
            }
            facilityType = "HOSPITAL";
        } else if (role == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() != null
                    && currentUser.getApprovalStatus() != User.ApprovalStatus.PENDING
                    && currentUser.getApprovalStatus() != User.ApprovalStatus.REJECTED) {
                targetBloodBankId = currentUser.getAssignedBloodBank().getId();
                facilityName = currentUser.getAssignedBloodBank().getName();
                facilityId = targetBloodBankId;
            } else {
                facilityName = "Unassigned Blood Bank Staff";
                facilityId = null;
            }
            facilityType = "BLOOD_BANK";
        }

        // 1. Resolve visible requests according to Module 5 & Module 6 rules
        List<BloodRequest> visibleRequests = getVisibleRequests(targetHospitalId, targetBloodBankId, role);

        // 2. Build Overview Stats
        OverviewStats overview = buildOverviewStats(targetHospitalId, targetBloodBankId, role, visibleRequests);

        // 3. Build Inventory Analytics (for BLOOD_BANK_STAFF and SYSTEM_ADMIN)
        InventoryAnalytics inventoryAnalytics = null;
        if (role == User.Role.BLOOD_BANK_STAFF || role == User.Role.SYSTEM_ADMIN) {
            if (role == User.Role.BLOOD_BANK_STAFF && targetBloodBankId == null) {
                inventoryAnalytics = InventoryAnalytics.builder()
                    .byBloodGroup(Collections.emptyList())
                    .byComponentType(Collections.emptyList())
                    .nearExpiryUnits(Collections.emptyList())
                    .build();
            } else {
                inventoryAnalytics = buildInventoryAnalytics(targetBloodBankId);
            }
        }

        // 4. Build Request Analytics (strictly computed from visibleRequests)
        RequestAnalytics requestAnalytics = buildRequestAnalytics(visibleRequests);

        // 5. Build Donation Analytics (for BLOOD_BANK_STAFF and SYSTEM_ADMIN)
        DonationAnalytics donationAnalytics = null;
        if (role == User.Role.BLOOD_BANK_STAFF || role == User.Role.SYSTEM_ADMIN) {
            if (role == User.Role.BLOOD_BANK_STAFF && targetBloodBankId == null) {
                donationAnalytics = DonationAnalytics.builder()
                    .totalCompletedDonations(0L)
                    .totalDonatedVolumeMl(0L)
                    .monthlyTrends(Collections.emptyList())
                    .byBloodGroup(Collections.emptyList())
                    .recentDonations(Collections.emptyList())
                    .build();
            } else {
                donationAnalytics = buildDonationAnalytics(targetBloodBankId);
            }
        }

        // 6. Build Operational Analytics (Dispatches)
        OperationalAnalytics operationalAnalytics;
        if ((role == User.Role.HOSPITAL_STAFF && targetHospitalId == null) ||
            (role == User.Role.BLOOD_BANK_STAFF && targetBloodBankId == null)) {
            operationalAnalytics = OperationalAnalytics.builder()
                .totalDispatches(0L)
                .activeInTransit(0L)
                .deliveredCount(0L)
                .recentDispatches(Collections.emptyList())
                .build();
        } else {
            operationalAnalytics = buildOperationalAnalytics(targetBloodBankId, targetHospitalId);
        }

        return DashboardAnalyticsDto.builder()
            .role(role.name())
            .username(currentUser.getUsername())
            .facilityId(facilityId)
            .facilityName(facilityName)
            .facilityType(facilityType)
            .overview(overview)
            .inventoryAnalytics(inventoryAnalytics)
            .requestAnalytics(requestAnalytics)
            .donationAnalytics(donationAnalytics)
            .operationalAnalytics(operationalAnalytics)
            .build();
    }

    /**
     * Reuses the exact visibility rules from BloodRequestService.getAllRequests:
     * - HOSPITAL_STAFF: requests belonging to their assigned hospital.
     * - BLOOD_BANK_STAFF: open unallocated requests + requests allocated to their assigned blood bank.
     *   (Requests allocated/dispatched by another blood bank are completely excluded).
     * - SYSTEM_ADMIN: all requests network-wide.
     */
    private List<BloodRequest> getVisibleRequests(Long hospitalId, Long bloodBankId, User.Role role) {
        if (role == User.Role.HOSPITAL_STAFF && hospitalId == null) {
            return Collections.emptyList();
        }
        if (role == User.Role.BLOOD_BANK_STAFF && bloodBankId == null) {
            return Collections.emptyList();
        }

        List<BloodRequest> requests;
        if (hospitalId != null) {
            requests = bloodRequestRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId);
        } else {
            requests = bloodRequestRepository.findAllByOrderByCreatedAtDesc();
        }

        if (role == User.Role.BLOOD_BANK_STAFF && bloodBankId != null) {
            requests = requests.stream()
                .filter(req -> {
                    List<BloodAllocation> allocs = bloodAllocationRepository.findAllByRequestId(req.getId());
                    if (allocs.isEmpty()) {
                        return true; // Open unallocated request (PENDING, REVIEWED, etc.)
                    }
                    // Processed request: strictly restricted to the allocating blood bank
                    return allocs.get(0).getBloodBank().getId().equals(bloodBankId);
                })
                .toList();
        }

        return requests;
    }

    private OverviewStats buildOverviewStats(Long hospitalId, Long bloodBankId, User.Role role, List<BloodRequest> visibleRequests) {
        OverviewStats stats = new OverviewStats();

        // Blood unit counts by status (Only relevant for blood banks and admins)
        if (role == User.Role.BLOOD_BANK_STAFF || role == User.Role.SYSTEM_ADMIN) {
            if (role == User.Role.BLOOD_BANK_STAFF && bloodBankId == null) {
                stats.setTotalUnits(0L);
                stats.setAvailableUnits(0L);
                stats.setReservedUnits(0L);
                stats.setIssuedUnits(0L);
                stats.setExpiredUnits(0L);
                stats.setDiscardedUnits(0L);
                stats.setQuarantinedUnits(0L);
                stats.setTotalDonors(0L);
                stats.setTotalDonations(0L);
            } else {
                List<Object[]> statusRows = bloodUnitRepository.countUnitsByStatus(bloodBankId);
                Map<BloodUnit.BloodUnitStatus, Long> unitStatusMap = new EnumMap<>(BloodUnit.BloodUnitStatus.class);
                long totalUnits = 0;
                for (Object[] row : statusRows) {
                    BloodUnit.BloodUnitStatus st = (BloodUnit.BloodUnitStatus) row[0];
                    Long count = ((Number) row[1]).longValue();
                    unitStatusMap.put(st, count);
                    totalUnits += count;
                }
                stats.setTotalUnits(totalUnits);
                stats.setAvailableUnits(unitStatusMap.getOrDefault(BloodUnit.BloodUnitStatus.AVAILABLE, 0L));
                stats.setReservedUnits(unitStatusMap.getOrDefault(BloodUnit.BloodUnitStatus.RESERVED, 0L));
                stats.setIssuedUnits(unitStatusMap.getOrDefault(BloodUnit.BloodUnitStatus.ISSUED, 0L));
                stats.setExpiredUnits(unitStatusMap.getOrDefault(BloodUnit.BloodUnitStatus.EXPIRED, 0L));
                stats.setDiscardedUnits(unitStatusMap.getOrDefault(BloodUnit.BloodUnitStatus.DISCARDED, 0L));
                stats.setQuarantinedUnits(unitStatusMap.getOrDefault(BloodUnit.BloodUnitStatus.QUARANTINED, 0L));

                stats.setTotalDonors(donorRepository.countDonors(bloodBankId));
                stats.setTotalDonations(donationRepository.countTotalDonations(bloodBankId));
            }
        }

        // Request counts strictly from visibleRequests
        Map<BloodRequest.RequestStatus, Long> reqStatusMap = new EnumMap<>(BloodRequest.RequestStatus.class);
        for (BloodRequest req : visibleRequests) {
            reqStatusMap.put(req.getStatus(), reqStatusMap.getOrDefault(req.getStatus(), 0L) + 1);
        }

        stats.setTotalRequests((long) visibleRequests.size());
        stats.setPendingRequests(reqStatusMap.getOrDefault(BloodRequest.RequestStatus.PENDING, 0L));
        stats.setReviewedRequests(reqStatusMap.getOrDefault(BloodRequest.RequestStatus.REVIEWED, 0L));
        stats.setAllocatedRequests(reqStatusMap.getOrDefault(BloodRequest.RequestStatus.ALLOCATED, 0L));
        stats.setDispatchedRequests(reqStatusMap.getOrDefault(BloodRequest.RequestStatus.DISPATCHED, 0L));
        stats.setFulfilledRequests(reqStatusMap.getOrDefault(BloodRequest.RequestStatus.FULFILLED, 0L));
        stats.setCancelledRequests(reqStatusMap.getOrDefault(BloodRequest.RequestStatus.CANCELLED, 0L));
        stats.setRejectedRequests(reqStatusMap.getOrDefault(BloodRequest.RequestStatus.REJECTED, 0L));

        // Active dispatches (Status == DISPATCHED)
        long activeDispatches = 0;
        if (!((role == User.Role.HOSPITAL_STAFF && hospitalId == null) || (role == User.Role.BLOOD_BANK_STAFF && bloodBankId == null))) {
            List<Object[]> dispRows = bloodDispatchRepository.countDispatchesByStatus(bloodBankId, hospitalId);
            for (Object[] row : dispRows) {
                BloodDispatch.DispatchStatus st = (BloodDispatch.DispatchStatus) row[0];
                if (st == BloodDispatch.DispatchStatus.DISPATCHED) {
                    activeDispatches = ((Number) row[1]).longValue();
                    break;
                }
            }
        }
        stats.setActiveDispatches(activeDispatches);

        return stats;
    }

    private InventoryAnalytics buildInventoryAnalytics(Long bloodBankId) {
        // By blood group (AVAILABLE units only)
        List<Object[]> groupRows = bloodUnitRepository.countAvailableUnitsByBloodType(bloodBankId);
        List<BloodGroupCount> byBloodGroup = new ArrayList<>();
        for (Object[] r : groupRows) {
            byBloodGroup.add(new BloodGroupCount(
                (BloodInventory.BloodType) r[0],
                ((Number) r[1]).longValue(),
                ((Number) r[2]).longValue()
            ));
        }

        // By component type (AVAILABLE units only)
        List<Object[]> compRows = bloodUnitRepository.countAvailableUnitsByComponentType(bloodBankId);
        List<ComponentCount> byComp = new ArrayList<>();
        for (Object[] r : compRows) {
            BloodUnit.BloodComponentType compType = (BloodUnit.BloodComponentType) r[0];
            byComp.add(new ComponentCount(
                compType,
                compType.getDisplayName(),
                ((Number) r[1]).longValue(),
                ((Number) r[2]).longValue()
            ));
        }

        // Near expiry units (next 7 days)
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(7);
        List<BloodUnit> nearExpiry = bloodUnitRepository.findUnitsNearExpiry(bloodBankId, today, threshold);
        List<UnitNearExpiryDto> nearExpiryDtos = nearExpiry.stream().map(u -> new UnitNearExpiryDto(
            u.getId(),
            u.getUnitId(),
            u.getBloodType(),
            u.getComponentType(),
            u.getQuantity(),
            u.getExpiryDate(),
            ChronoUnit.DAYS.between(today, u.getExpiryDate()),
            u.getBloodBank() != null ? u.getBloodBank().getName() : "Unknown"
        )).toList();

        return InventoryAnalytics.builder()
            .byBloodGroup(byBloodGroup)
            .byComponentType(byComp)
            .nearExpiryUnits(nearExpiryDtos)
            .build();
    }

    private RequestAnalytics buildRequestAnalytics(List<BloodRequest> visibleRequests) {
        Map<BloodRequest.RequestStatus, Long> statusMap = new EnumMap<>(BloodRequest.RequestStatus.class);
        Map<BloodRequest.RequestPriority, Long> priorityMap = new EnumMap<>(BloodRequest.RequestPriority.class);

        long fulfilledCount = 0;
        long eligibleCount = 0;

        for (BloodRequest req : visibleRequests) {
            BloodRequest.RequestStatus st = req.getStatus();
            statusMap.put(st, statusMap.getOrDefault(st, 0L) + 1);

            BloodRequest.RequestPriority prio = req.getPriority();
            if (prio != null) {
                priorityMap.put(prio, priorityMap.getOrDefault(prio, 0L) + 1);
            }

            // Fulfillment Rate rule: Exclude CANCELLED and REJECTED from the denominator
            if (st != BloodRequest.RequestStatus.CANCELLED && st != BloodRequest.RequestStatus.REJECTED) {
                eligibleCount++;
            }
            if (st == BloodRequest.RequestStatus.FULFILLED) {
                fulfilledCount++;
            }
        }

        List<StatusCount> byStatus = statusMap.entrySet().stream()
            .map(e -> new StatusCount(e.getKey(), e.getValue()))
            .toList();

        List<PriorityCount> byPrio = priorityMap.entrySet().stream()
            .map(e -> new PriorityCount(e.getKey(), e.getValue()))
            .toList();

        // Safe fulfillment rate calculation
        double rate = eligibleCount > 0
            ? Math.round(((double) fulfilledCount / eligibleCount) * 1000.0) / 10.0
            : 0.0;

        // Recent requests: top 6 from the strictly scoped visibleRequests
        List<RecentRequestDto> recentDtos = visibleRequests.stream()
            .limit(6)
            .map(req -> new RecentRequestDto(
                req.getId(),
                req.getHospital() != null ? req.getHospital().getName() : "Unknown",
                req.getBloodType(),
                req.getComponentType(),
                req.getQuantity(),
                req.getPriority(),
                req.getStatus(),
                req.getRequiredDate(),
                req.getCreatedAt()
            )).toList();

        return RequestAnalytics.builder()
            .byStatus(byStatus)
            .byPriority(byPrio)
            .recentRequests(recentDtos)
            .fulfillmentRatePercent(rate)
            .eligibleRequestsCount(eligibleCount)
            .fulfilledRequestsCount(fulfilledCount)
            .build();
    }

    private DonationAnalytics buildDonationAnalytics(Long bloodBankId) {
        List<Object[]> totals = donationRepository.getCompletedDonationTotals(bloodBankId);
        long completedCount = 0;
        long totalVolume = 0;
        if (!totals.isEmpty() && totals.get(0)[0] != null) {
            completedCount = ((Number) totals.get(0)[0]).longValue();
            totalVolume = totals.get(0)[1] != null ? ((Number) totals.get(0)[1]).longValue() : 0;
        }

        // By blood group
        List<Object[]> groupRows = donationRepository.getCompletedDonationsByBloodType(bloodBankId);
        List<DonationByGroup> byGroup = new ArrayList<>();
        for (Object[] r : groupRows) {
            byGroup.add(new DonationByGroup(
                (BloodInventory.BloodType) r[0],
                ((Number) r[1]).longValue(),
                ((Number) r[2]).longValue()
            ));
        }

        // Monthly trends: strictly latest 6 calendar months (including months with 0 donations)
        YearMonth currentYm = YearMonth.now();
        List<YearMonth> last6Months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            last6Months.add(currentYm.minusMonths(i));
        }

        LocalDate sixMonthsAgoDate = last6Months.get(0).atDay(1);
        List<Object[]> donationsSince = donationRepository.getCompletedDonationsSince(bloodBankId, sixMonthsAgoDate);

        Map<String, long[]> monthAggregates = new HashMap<>(); // "YYYY-MM" -> [count, volume]
        for (Object[] row : donationsSince) {
            LocalDate dDate = (LocalDate) row[0];
            int vol = ((Number) row[1]).intValue();
            String ymKey = dDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            monthAggregates.computeIfAbsent(ymKey, k -> new long[]{0, 0});
            monthAggregates.get(ymKey)[0]++;
            monthAggregates.get(ymKey)[1] += vol;
        }

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        List<MonthlyDonationTrend> monthlyTrends = new ArrayList<>();
        for (YearMonth ym : last6Months) {
            String ymKey = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            long[] agg = monthAggregates.getOrDefault(ymKey, new long[]{0, 0});
            monthlyTrends.add(new MonthlyDonationTrend(
                ymKey,
                ym.format(labelFormatter),
                agg[0],
                agg[1]
            ));
        }

        // Recent donations (Top 5)
        List<Donation> recents = donationRepository.findRecentDonations(bloodBankId, PageRequest.of(0, 5));
        List<RecentDonationDto> recentDtos = recents.stream().map(d -> new RecentDonationDto(
            d.getId(),
            d.getDonor() != null ? d.getDonor().getFullName() : "Anonymous",
            d.getBloodType(),
            d.getQuantity(),
            d.getDonationDate(),
            d.getStatus().name(),
            d.getBloodBank() != null ? d.getBloodBank().getName() : "Unknown"
        )).toList();

        return DonationAnalytics.builder()
            .totalCompletedDonations(completedCount)
            .totalDonatedVolumeMl(totalVolume)
            .monthlyTrends(monthlyTrends)
            .byBloodGroup(byGroup)
            .recentDonations(recentDtos)
            .build();
    }

    private OperationalAnalytics buildOperationalAnalytics(Long bloodBankId, Long hospitalId) {
        List<Object[]> statusRows = bloodDispatchRepository.countDispatchesByStatus(bloodBankId, hospitalId);
        long totalDispatches = 0;
        long activeInTransit = 0;
        long deliveredCount = 0;

        for (Object[] r : statusRows) {
            BloodDispatch.DispatchStatus st = (BloodDispatch.DispatchStatus) r[0];
            long count = ((Number) r[1]).longValue();
            totalDispatches += count;
            if (st == BloodDispatch.DispatchStatus.DISPATCHED) {
                activeInTransit += count;
            } else if (st == BloodDispatch.DispatchStatus.DELIVERED) {
                deliveredCount += count;
            }
        }

        List<BloodDispatch> recents = bloodDispatchRepository.findRecentDispatches(bloodBankId, hospitalId, PageRequest.of(0, 5));
        List<RecentDispatchDto> recentDtos = recents.stream().map(d -> new RecentDispatchDto(
            d.getId(),
            d.getDispatchReference(),
            d.getHospital() != null ? d.getHospital().getName() : "Unknown",
            d.getBloodBank() != null ? d.getBloodBank().getName() : "Unknown",
            d.getDispatchDate(),
            d.getStatus().name(),
            d.getUnits() != null ? d.getUnits().size() : 0
        )).toList();

        return OperationalAnalytics.builder()
            .totalDispatches(totalDispatches)
            .activeInTransit(activeInTransit)
            .deliveredCount(deliveredCount)
            .recentDispatches(recentDtos)
            .build();
    }
}
