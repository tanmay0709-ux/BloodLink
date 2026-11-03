package com.bloodlink.service;

import com.bloodlink.dto.BloodUnitRequest;
import com.bloodlink.dto.BloodUnitResponse;
import com.bloodlink.dto.StockSummaryDto;
import com.bloodlink.model.*;
import com.bloodlink.repository.BloodBankRepository;
import com.bloodlink.repository.BloodUnitRepository;
import com.bloodlink.repository.DonationRepository;
import com.bloodlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BloodUnitService {

    private final BloodUnitRepository bloodUnitRepository;
    private final DonationRepository donationRepository;
    private final BloodBankRepository bloodBankRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("User not found: " + auth.getName()));
    }

    private Long getEnforcedBloodBankId(User currentUser) {
        if (currentUser.getRole() == User.Role.SYSTEM_ADMIN) {
            return null; // admin has global access
        } else if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                throw new AccessDeniedException("Staff member is not an approved member of any blood bank");
            }
            return currentUser.getAssignedBloodBank().getId();
        } else {
            throw new AccessDeniedException("Access denied: Hospital staff cannot manage blood unit inventory");
        }
    }

    @Transactional
    public void checkAndMarkExpiredUnits() {
        LocalDate today = LocalDate.now();
        List<BloodUnit> expiredUnits = bloodUnitRepository.findExpiredUnits(today);
        for (BloodUnit unit : expiredUnits) {
            unit.setStatus(BloodUnit.BloodUnitStatus.EXPIRED);
        }
        if (!expiredUnits.isEmpty()) {
            bloodUnitRepository.saveAll(expiredUnits);
        }
    }

    @Transactional
    public List<BloodUnitResponse> getAll(BloodInventory.BloodType bloodType,
                                          BloodUnit.BloodComponentType componentType,
                                          BloodUnit.BloodUnitStatus status,
                                          String query,
                                          Long requestedBloodBankId) {
        checkAndMarkExpiredUnits();

        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);
        Long targetBankId = enforcedId != null ? enforcedId : requestedBloodBankId;

        String q = (query != null && !query.trim().isEmpty()) ? query.trim() : null;

        List<BloodUnit> units;
        if (bloodType == null && componentType == null && status == null && q == null) {
            units = (targetBankId != null)
                ? bloodUnitRepository.findAllByBloodBankId(targetBankId)
                : bloodUnitRepository.findAll();
        } else {
            units = bloodUnitRepository.searchUnits(
                targetBankId, bloodType, componentType, status, q
            );
        }
        return units.stream().map(BloodUnitResponse::fromEntity).toList();
    }

    @Transactional
    public BloodUnitResponse getById(Long id) {
        checkAndMarkExpiredUnits();

        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        BloodUnit unit = bloodUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Blood unit not found with id: " + id));

        if (enforcedId != null && !unit.getBloodBank().getId().equals(enforcedId)) {
            throw new AccessDeniedException("Access denied: You can only access blood units belonging to your assigned blood bank");
        }

        return BloodUnitResponse.fromEntity(unit);
    }

    @Transactional
    public BloodUnitResponse create(BloodUnitRequest request) {
        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        Donation donation = donationRepository.findById(request.getDonationId())
            .orElseThrow(() -> new IllegalArgumentException("Donation not found with id: " + request.getDonationId()));

        // Business rule: Only COMPLETED donations can generate inventory
        if (donation.getStatus() != Donation.DonationStatus.COMPLETED) {
            throw new IllegalArgumentException("Only COMPLETED donations can generate blood unit inventory. Current donation status: " + donation.getStatus());
        }

        // Blood bank scoping
        BloodBank bloodBank = donation.getBloodBank();
        if (enforcedId != null && !bloodBank.getId().equals(enforcedId)) {
            throw new AccessDeniedException("Access denied: You can only create blood units for donations at your assigned blood bank");
        }

        if (currentUser.getRole() == User.Role.SYSTEM_ADMIN && request.getBloodBankId() != null) {
            bloodBank = bloodBankRepository.findById(request.getBloodBankId())
                .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + request.getBloodBankId()));
        }

        // Generate or validate unit identifier
        String unitId = request.getUnitId();
        if (unitId != null && !unitId.trim().isEmpty()) {
            unitId = unitId.trim().toUpperCase();
            if (bloodUnitRepository.existsByUnitId(unitId)) {
                throw new IllegalArgumentException("A blood unit with ID '" + unitId + "' already exists");
            }
        } else {
            // Auto-generate unit ID: BU-<donationId>-<shortComponent>-<timestamp4>
            String compCode = request.getComponentType().name().substring(0, Math.min(3, request.getComponentType().name().length()));
            unitId = String.format("BU-%d-%s-%04d", donation.getId(), compCode, (System.currentTimeMillis() % 10000));
        }

        // Collection date & expiry calculation
        LocalDate collectionDate = request.getCollectionDate() != null ? request.getCollectionDate() : donation.getDonationDate();
        if (collectionDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Collection date cannot be in the future");
        }

        LocalDate expiryDate = request.getExpiryDate();
        if (expiryDate == null) {
            expiryDate = collectionDate.plusDays(request.getComponentType().getStandardShelfLifeDays());
        }

        // Initial status
        BloodUnit.BloodUnitStatus status = request.getStatus() != null ? request.getStatus() : BloodUnit.BloodUnitStatus.AVAILABLE;
        if (expiryDate.isBefore(LocalDate.now())) {
            status = BloodUnit.BloodUnitStatus.EXPIRED;
        }

        BloodUnit unit = BloodUnit.builder()
            .unitId(unitId)
            .donation(donation)
            .bloodBank(bloodBank)
            .bloodType(donation.getBloodType())
            .componentType(request.getComponentType())
            .quantity(request.getQuantity())
            .collectionDate(collectionDate)
            .expiryDate(expiryDate)
            .status(status)
            .notes(request.getNotes())
            .build();

        BloodUnit saved = bloodUnitRepository.save(unit);
        return BloodUnitResponse.fromEntity(saved);
    }

    @Transactional
    public BloodUnitResponse update(Long id, BloodUnitRequest request) {
        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        BloodUnit unit = bloodUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Blood unit not found with id: " + id));

        if (enforcedId != null && !unit.getBloodBank().getId().equals(enforcedId)) {
            throw new AccessDeniedException("Access denied: You can only update blood units belonging to your assigned blood bank");
        }

        if (request.getUnitId() != null && !request.getUnitId().trim().isEmpty()) {
            String newUnitId = request.getUnitId().trim().toUpperCase();
            if (bloodUnitRepository.existsByUnitIdAndIdNot(newUnitId, id)) {
                throw new IllegalArgumentException("Another blood unit with ID '" + newUnitId + "' already exists");
            }
            unit.setUnitId(newUnitId);
        }

        if (request.getQuantity() != null) {
            unit.setQuantity(request.getQuantity());
        }
        if (request.getNotes() != null) {
            unit.setNotes(request.getNotes());
        }
        if (request.getExpiryDate() != null) {
            unit.setExpiryDate(request.getExpiryDate());
            if (unit.getExpiryDate().isBefore(LocalDate.now()) && unit.getStatus() == BloodUnit.BloodUnitStatus.AVAILABLE) {
                unit.setStatus(BloodUnit.BloodUnitStatus.EXPIRED);
            }
        }

        return BloodUnitResponse.fromEntity(bloodUnitRepository.save(unit));
    }

    @Transactional
    public BloodUnitResponse updateStatus(Long id, BloodUnit.BloodUnitStatus newStatus) {
        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        BloodUnit unit = bloodUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Blood unit not found with id: " + id));

        if (enforcedId != null && !unit.getBloodBank().getId().equals(enforcedId)) {
            throw new AccessDeniedException("Access denied: You can only update blood units belonging to your assigned blood bank");
        }

        BloodUnit.BloodUnitStatus currentStatus = unit.getStatus();
        if (currentStatus == newStatus) {
            return BloodUnitResponse.fromEntity(unit);
        }

        // Terminal state checks
        if (currentStatus == BloodUnit.BloodUnitStatus.ISSUED) {
            throw new IllegalArgumentException("Invalid status transition: ISSUED units cannot be modified");
        }
        if (currentStatus == BloodUnit.BloodUnitStatus.DISCARDED) {
            throw new IllegalArgumentException("Invalid status transition: DISCARDED units cannot be modified");
        }

        // Expired check: prevent allocation or re-availability of expired units
        boolean isExpired = unit.getExpiryDate().isBefore(LocalDate.now()) || currentStatus == BloodUnit.BloodUnitStatus.EXPIRED;
        if (isExpired) {
            if (newStatus == BloodUnit.BloodUnitStatus.AVAILABLE ||
                newStatus == BloodUnit.BloodUnitStatus.RESERVED ||
                newStatus == BloodUnit.BloodUnitStatus.ISSUED) {
                throw new IllegalArgumentException("Cannot allocate or make available an expired blood unit (Expired on: " + unit.getExpiryDate() + "). It can only be DISCARDED.");
            }
        }

        // Discarded transition allowed from any non-terminal state
        // Valid transitions from EXPIRED is only DISCARDED
        if (currentStatus == BloodUnit.BloodUnitStatus.EXPIRED && newStatus != BloodUnit.BloodUnitStatus.DISCARDED) {
            throw new IllegalArgumentException("EXPIRED units can only be transitioned to DISCARDED");
        }

        unit.setStatus(newStatus);
        return BloodUnitResponse.fromEntity(bloodUnitRepository.save(unit));
    }

    @Transactional
    public StockSummaryDto getStockSummary(Long requestedBloodBankId) {
        checkAndMarkExpiredUnits();

        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);
        Long targetBankId = enforcedId != null ? enforcedId : requestedBloodBankId;

        List<BloodUnit> allUnits = (targetBankId != null)
            ? bloodUnitRepository.findAllByBloodBankId(targetBankId)
            : bloodUnitRepository.findAll();

        StockSummaryDto summary = new StockSummaryDto();
        summary.setTotalUnits(allUnits.size());

        Map<BloodInventory.BloodType, Long> availableMap = new HashMap<>();
        Map<BloodInventory.BloodType, Long> quantityMap = new HashMap<>();

        for (BloodInventory.BloodType bt : BloodInventory.BloodType.values()) {
            availableMap.put(bt, 0L);
            quantityMap.put(bt, 0L);
        }

        long available = 0, reserved = 0, issued = 0, expired = 0, discarded = 0, quarantined = 0;

        for (BloodUnit u : allUnits) {
            switch (u.getStatus()) {
                case AVAILABLE -> {
                    available++;
                    availableMap.put(u.getBloodType(), availableMap.get(u.getBloodType()) + 1);
                    quantityMap.put(u.getBloodType(), quantityMap.get(u.getBloodType()) + u.getQuantity());
                }
                case RESERVED -> reserved++;
                case ISSUED -> issued++;
                case EXPIRED -> expired++;
                case DISCARDED -> discarded++;
                case QUARANTINED -> quarantined++;
            }
        }

        summary.setAvailableUnits(available);
        summary.setReservedUnits(reserved);
        summary.setIssuedUnits(issued);
        summary.setExpiredUnits(expired);
        summary.setDiscardedUnits(discarded);
        summary.setQuarantinedUnits(quarantined);
        summary.setAvailableByBloodType(availableMap);
        summary.setQuantityMlByBloodType(quantityMap);

        return summary;
    }
}
