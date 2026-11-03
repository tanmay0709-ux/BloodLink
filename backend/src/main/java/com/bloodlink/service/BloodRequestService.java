package com.bloodlink.service;

import com.bloodlink.dto.BloodRequestCreateDto;
import com.bloodlink.dto.BloodRequestResponse;
import com.bloodlink.dto.BloodUnitResponse;
import com.bloodlink.dto.MatchingInventoryDto;
import com.bloodlink.model.*;
import com.bloodlink.repository.BloodAllocationRepository;
import com.bloodlink.repository.BloodBankRepository;
import com.bloodlink.repository.BloodRequestRepository;
import com.bloodlink.repository.BloodUnitRepository;
import com.bloodlink.repository.HospitalRepository;
import com.bloodlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodBankRepository bloodBankRepository;
    private final BloodUnitRepository bloodUnitRepository;
    private final UserRepository userRepository;
    private final BloodCompatibilityService bloodCompatibilityService;
    private final BloodUnitService bloodUnitService;
    private final BloodAllocationRepository bloodAllocationRepository;
    private final NotificationService notificationService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("User not found: " + auth.getName()));
    }

    @Transactional
    public BloodRequestResponse createRequest(BloodRequestCreateDto dto) {
        User currentUser = getCurrentUser();

        Hospital hospital;
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                throw new AccessDeniedException("Staff member is not an approved member of any hospital");
            }
            hospital = currentUser.getAssignedHospital();
        } else if (currentUser.getRole() == User.Role.SYSTEM_ADMIN) {
            if (dto.getHospitalId() == null) {
                hospital = hospitalRepository.findAllByActiveTrue().stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No active hospital found. Please provide a valid hospitalId."));
            } else {
                hospital = hospitalRepository.findById(dto.getHospitalId())
                    .orElseThrow(() -> new IllegalArgumentException("Hospital not found with id: " + dto.getHospitalId()));
            }
        } else {
            throw new AccessDeniedException("Access denied: Only hospital staff and administrators can create blood requests");
        }

        if (!hospital.getActive()) {
            throw new IllegalArgumentException("Cannot create a request for an inactive hospital");
        }

        // Validate required date
        if (dto.getRequiredDate() == null || dto.getRequiredDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Required date cannot be in the past");
        }

        // Validate quantity
        if (dto.getQuantity() == null || dto.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1 unit");
        }

        BloodRequest request = BloodRequest.builder()
            .hospital(hospital)
            .bloodType(dto.getBloodType())
            .componentType(dto.getComponentType())
            .quantity(dto.getQuantity())
            .priority(dto.getPriority() != null ? dto.getPriority() : BloodRequest.RequestPriority.ROUTINE)
            .requiredDate(dto.getRequiredDate())
            .status(BloodRequest.RequestStatus.PENDING)
            .createdBy(currentUser)
            .clinicalReason(dto.getClinicalReason())
            .build();

        BloodRequest saved = bloodRequestRepository.save(request);

        // 1. REQUEST_CREATED Notification: Broadcast to all active blood bank staff & system admins
        String notifTitle = "New Blood Request #" + saved.getId();
        String notifMsg = String.format("%s requested %d unit(s) of %s %s (Priority: %s)",
            hospital.getName(), saved.getQuantity(), saved.getBloodType().name(),
            saved.getComponentType().getDisplayName(), saved.getPriority().name());

        notificationService.notifyAllBloodBankStaff(notifTitle, notifMsg, NotificationType.REQUEST_CREATED, saved.getId(), "BLOOD_REQUEST");
        notificationService.notifyAdmins(notifTitle, notifMsg, NotificationType.REQUEST_CREATED, saved.getId(), "BLOOD_REQUEST");

        return BloodRequestResponse.fromEntity(saved);
    }

    public List<BloodRequestResponse> getAllRequests(Long requestedHospitalId,
                                                     BloodRequest.RequestStatus status,
                                                     BloodRequest.RequestPriority priority) {
        User currentUser = getCurrentUser();

        Long targetHospitalId = null;
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                return List.of();
            }
            targetHospitalId = currentUser.getAssignedHospital().getId();
        } else {
            targetHospitalId = requestedHospitalId;
        }

        List<BloodRequest> requests;
        if (targetHospitalId == null && status == null && priority == null) {
            requests = bloodRequestRepository.findAllByOrderByCreatedAtDesc();
        } else {
            requests = bloodRequestRepository.findWithFilters(targetHospitalId, status, priority);
        }

        // For BLOOD_BANK_STAFF: hide requests that have been allocated/dispatched by another blood bank
        if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                return List.of();
            }
            Long staffBankId = currentUser.getAssignedBloodBank().getId();
            requests = requests.stream()
                .filter(req -> {
                    // Open requests (PENDING, REVIEWED, CANCELLED, REJECTED) or requests without allocations remain visible
                    List<BloodAllocation> allocs = bloodAllocationRepository.findAllByRequestId(req.getId());
                    if (allocs.isEmpty()) {
                        return true;
                    }
                    // If allocated/dispatched, only visible to the assigned blood bank that owns the allocation
                    return allocs.get(0).getBloodBank().getId().equals(staffBankId);
                })
                .toList();
        }

        return requests.stream().map(BloodRequestResponse::fromEntity).toList();
    }

    public BloodRequestResponse getRequestById(Long id) {
        User currentUser = getCurrentUser();

        BloodRequest request = bloodRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Blood request not found with id: " + id));

        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !request.getHospital().getId().equals(currentUser.getAssignedHospital().getId())) {
                throw new AccessDeniedException("Access denied: You can only view requests from your assigned hospital");
            }
        } else if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                throw new AccessDeniedException("Staff member is not an approved member of any blood bank");
            }
            Long staffBankId = currentUser.getAssignedBloodBank().getId();
            List<BloodAllocation> allocs = bloodAllocationRepository.findAllByRequestId(request.getId());
            if (!allocs.isEmpty() && !allocs.get(0).getBloodBank().getId().equals(staffBankId)) {
                throw new AccessDeniedException("Access denied: This request is allocated to another blood bank");
            }
        }

        return BloodRequestResponse.fromEntity(request);
    }

    @Transactional
    public BloodRequestResponse updateStatus(Long id, BloodRequest.RequestStatus newStatus) {
        User currentUser = getCurrentUser();

        BloodRequest request = bloodRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Blood request not found with id: " + id));

        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !request.getHospital().getId().equals(currentUser.getAssignedHospital().getId())) {
                throw new AccessDeniedException("Access denied: You can only update requests belonging to your assigned hospital");
            }
            // Hospital staff can only cancel pending requests
            if (newStatus != BloodRequest.RequestStatus.CANCELLED) {
                throw new IllegalArgumentException("Hospital staff can only set request status to CANCELLED");
            }
            if (request.getStatus() != BloodRequest.RequestStatus.PENDING) {
                throw new IllegalArgumentException("Only PENDING requests can be cancelled");
            }
        } else if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                throw new AccessDeniedException("Staff member is not an approved member of any blood bank");
            }
            Long staffBankId = currentUser.getAssignedBloodBank().getId();

            // A blood bank must not modify a request already allocated to another blood bank
            List<BloodAllocation> allocs = bloodAllocationRepository.findAllByRequestId(request.getId());
            if (!allocs.isEmpty() && !allocs.get(0).getBloodBank().getId().equals(staffBankId)) {
                throw new AccessDeniedException("Access denied: This request is allocated to another blood bank");
            }

            // Enforce valid status transitions for blood bank staff
            // Blood bank staff can mark PENDING as REVIEWED or REJECTED.
            // Direct jumps to ALLOCATED, DISPATCHED, or FULFILLED must happen through the dedicated allocation/dispatch flows.
            if (newStatus == BloodRequest.RequestStatus.REVIEWED || newStatus == BloodRequest.RequestStatus.REJECTED) {
                if (request.getStatus() != BloodRequest.RequestStatus.PENDING && request.getStatus() != BloodRequest.RequestStatus.REVIEWED) {
                    throw new IllegalArgumentException("Cannot update status to " + newStatus + " from current status " + request.getStatus());
                }
            } else if (newStatus == BloodRequest.RequestStatus.CANCELLED) {
                throw new IllegalArgumentException("Only hospital staff or administrators can cancel a request");
            } else if (newStatus == BloodRequest.RequestStatus.ALLOCATED || newStatus == BloodRequest.RequestStatus.DISPATCHED || newStatus == BloodRequest.RequestStatus.FULFILLED) {
                throw new IllegalArgumentException("Status " + newStatus + " must be transitioned through the appropriate allocation or dispatch action");
            }
        } else if (currentUser.getRole() != User.Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException("Access denied: You do not have permission to update request status");
        }

        request.setStatus(newStatus);
        BloodRequest saved = bloodRequestRepository.save(request);

        // Notifications based on status transition
        if (newStatus == BloodRequest.RequestStatus.REVIEWED) {
            String title = "Request #" + saved.getId() + " Under Review";
            String msg = String.format("Request #%d for %s %s has been reviewed by blood bank staff and is being prepared for fulfillment.",
                saved.getId(), saved.getBloodType().name(), saved.getComponentType().getDisplayName());
            notificationService.notifyHospitalStaff(saved.getHospital(), title, msg, NotificationType.REQUEST_REVIEWED, saved.getId(), "BLOOD_REQUEST");
            notificationService.notifyAdmins(title, msg, NotificationType.REQUEST_REVIEWED, saved.getId(), "BLOOD_REQUEST");

        } else if (newStatus == BloodRequest.RequestStatus.REJECTED) {
            String title = "Request #" + saved.getId() + " Rejected";
            String msg = String.format("Request #%d for %s %s has been rejected by blood bank staff.",
                saved.getId(), saved.getBloodType().name(), saved.getComponentType().getDisplayName());
            notificationService.notifyHospitalStaff(saved.getHospital(), title, msg, NotificationType.REQUEST_REJECTED, saved.getId(), "BLOOD_REQUEST");
            notificationService.notifyAdmins(title, msg, NotificationType.REQUEST_REJECTED, saved.getId(), "BLOOD_REQUEST");

        } else if (newStatus == BloodRequest.RequestStatus.CANCELLED) {
            String title = "Request #" + saved.getId() + " Cancelled";
            String msg = String.format("Request #%d has been cancelled by %s.",
                saved.getId(), saved.getHospital().getName());
            
            // If request had an allocation or an assigned reviewing bank, notify that bank; otherwise admins
            List<BloodAllocation> allocs = bloodAllocationRepository.findAllByRequestId(saved.getId());
            if (!allocs.isEmpty()) {
                notificationService.notifyBloodBankStaff(allocs.get(0).getBloodBank(), title, msg, NotificationType.REQUEST_CANCELLED, saved.getId(), "BLOOD_REQUEST");
            } else {
                notificationService.notifyAllBloodBankStaff(title, msg, NotificationType.REQUEST_CANCELLED, saved.getId(), "BLOOD_REQUEST");
            }
            notificationService.notifyAdmins(title, msg, NotificationType.REQUEST_CANCELLED, saved.getId(), "BLOOD_REQUEST");
        }

        return BloodRequestResponse.fromEntity(saved);
    }

    @Transactional
    public MatchingInventoryDto getMatchingInventory(Long requestId, Long requestedBloodBankId) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != User.Role.BLOOD_BANK_STAFF && currentUser.getRole() != User.Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException("Access denied: Only blood bank staff and administrators can view matching inventory");
        }

        Long targetBankId = null;
        if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                throw new AccessDeniedException("Staff member is not an approved member of any blood bank");
            }
            targetBankId = currentUser.getAssignedBloodBank().getId();
        } else {
            targetBankId = requestedBloodBankId;
        }

        // Trigger expiration update before scanning matching inventory
        bloodUnitService.checkAndMarkExpiredUnits();

        BloodRequest request = bloodRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Blood request not found with id: " + requestId));

        // Get compatible blood types (applies educational ABO/Rh compatibility for RED_BLOOD_CELLS only)
        List<BloodInventory.BloodType> compatibleTypes =
            bloodCompatibilityService.getCompatibleDonorBloodTypes(request.getBloodType(), request.getComponentType());

        // Find available and unexpired units from inventory
        List<BloodUnit> allUnits = (targetBankId != null)
            ? bloodUnitRepository.findAllByBloodBankId(targetBankId)
            : bloodUnitRepository.findAll();

        LocalDate today = LocalDate.now();
        List<BloodUnitResponse> matchingUnits = allUnits.stream()
            .filter(u -> u.getStatus() == BloodUnit.BloodUnitStatus.AVAILABLE)
            .filter(u -> !u.getExpiryDate().isBefore(today))
            .filter(u -> u.getComponentType() == request.getComponentType())
            .filter(u -> compatibleTypes.contains(u.getBloodType()))
            .map(BloodUnitResponse::fromEntity)
            .toList();

        MatchingInventoryDto result = new MatchingInventoryDto();
        result.setRequestId(request.getId());
        result.setRecipientBloodType(request.getBloodType());
        result.setComponentType(request.getComponentType());
        result.setCompatibleBloodTypes(compatibleTypes);
        result.setDisclaimer(BloodCompatibilityService.CLINICAL_DISCLAIMER);
        result.setMatchingUnits(matchingUnits);
        result.setTotalAvailableUnits(matchingUnits.size());

        return result;
    }
}
