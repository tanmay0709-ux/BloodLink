package com.bloodlink.service;

import com.bloodlink.dto.*;
import com.bloodlink.model.*;
import com.bloodlink.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BloodAllocationService {

    private final BloodRequestRepository bloodRequestRepository;
    private final BloodUnitRepository bloodUnitRepository;
    private final BloodAllocationRepository bloodAllocationRepository;
    private final BloodDispatchRepository bloodDispatchRepository;
    private final BloodBankRepository bloodBankRepository;
    private final UserRepository userRepository;
    private final BloodCompatibilityService bloodCompatibilityService;
    private final NotificationService notificationService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("User not found: " + auth.getName()));
    }

    /**
     * Stage 1: ALLOCATION
     * Only BLOOD_BANK_STAFF and SYSTEM_ADMIN can allocate blood units to REVIEWED requests.
     * Uses pessimistic locking to prevent race conditions.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<AllocationResponseDto> allocateUnits(Long requestId, AllocationRequestDto dto) {
        User currentUser = getCurrentUser();

        // Check authorized roles
        if (currentUser.getRole() != User.Role.BLOOD_BANK_STAFF && currentUser.getRole() != User.Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException("Access denied: Only blood bank staff and administrators can allocate blood units");
        }

        // Determine blood bank
        BloodBank bloodBank;
        if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                throw new AccessDeniedException("Staff member is not an approved member of any blood bank");
            }
            bloodBank = currentUser.getAssignedBloodBank();
        } else {
            if (dto.getBloodBankId() == null) {
                bloodBank = bloodBankRepository.findAllByActiveTrue().stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No active blood bank found"));
            } else {
                bloodBank = bloodBankRepository.findById(dto.getBloodBankId())
                    .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + dto.getBloodBankId()));
            }
        }

        // 1. Verify request exists and is in REVIEWED status
        BloodRequest request = bloodRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Blood request not found with id: " + requestId));

        if (request.getStatus() != BloodRequest.RequestStatus.REVIEWED) {
            throw new IllegalArgumentException("Only REVIEWED requests can be allocated. Current status: " + request.getStatus());
        }

        // 2. Validate selected units fulfill the requested quantity
        if (dto.getUnitIds() == null || dto.getUnitIds().isEmpty()) {
            throw new IllegalArgumentException("At least one blood unit must be selected for allocation");
        }
        if (dto.getUnitIds().size() != request.getQuantity()) {
            throw new IllegalArgumentException("Selected " + dto.getUnitIds().size() + " unit(s), but request #"
                + requestId + " requires exactly " + request.getQuantity() + " unit(s).");
        }

        // 3. Acquire pessimistic write lock on selected units to prevent concurrent allocations
        List<BloodUnit> lockedUnits = bloodUnitRepository.findAllByIdWithLock(dto.getUnitIds());
        if (lockedUnits.size() != dto.getUnitIds().size()) {
            throw new IllegalArgumentException("One or more selected blood units were not found in inventory");
        }

        // 4. Compatibility check
        List<BloodInventory.BloodType> compatibleTypes =
            bloodCompatibilityService.getCompatibleDonorBloodTypes(request.getBloodType(), request.getComponentType());

        LocalDate today = LocalDate.now();

        // 5. Validate each unit against strict business rules
        for (BloodUnit unit : lockedUnits) {
            // Ownership check
            if (!unit.getBloodBank().getId().equals(bloodBank.getId())) {
                throw new IllegalArgumentException("Unit " + unit.getUnitId() + " belongs to '"
                    + unit.getBloodBank().getName() + "', not '" + bloodBank.getName() + "'");
            }

            // Status check
            if (unit.getStatus() != BloodUnit.BloodUnitStatus.AVAILABLE) {
                throw new IllegalArgumentException("Blood unit " + unit.getUnitId()
                    + " cannot be allocated because its status is " + unit.getStatus() + " (must be AVAILABLE)");
            }

            // Expiry check
            if (unit.getExpiryDate().isBefore(today)) {
                throw new IllegalArgumentException("Blood unit " + unit.getUnitId()
                    + " is EXPIRED (Expired on: " + unit.getExpiryDate() + ") and cannot be allocated");
            }

            // Component type check
            if (unit.getComponentType() != request.getComponentType()) {
                throw new IllegalArgumentException("Blood unit " + unit.getUnitId()
                    + " has component type " + unit.getComponentType() + ", but request requires " + request.getComponentType());
            }

            // ABO/Rh compatibility check
            if (!compatibleTypes.contains(unit.getBloodType())) {
                throw new IllegalArgumentException("Blood unit " + unit.getUnitId()
                    + " with blood group " + unit.getBloodType() + " is incompatible with recipient group "
                    + request.getBloodType() + " for " + request.getComponentType());
            }
        }

        // 6. Apply allocation changes
        List<BloodAllocation> savedAllocations = new ArrayList<>();
        for (BloodUnit unit : lockedUnits) {
            unit.setStatus(BloodUnit.BloodUnitStatus.RESERVED);
            bloodUnitRepository.save(unit);

            BloodAllocation allocation = BloodAllocation.builder()
                .request(request)
                .bloodUnit(unit)
                .bloodBank(bloodBank)
                .allocatedBy(currentUser)
                .status(BloodAllocation.AllocationStatus.ALLOCATED)
                .build();
            savedAllocations.add(bloodAllocationRepository.save(allocation));
        }

        // Update request status to ALLOCATED
        request.setStatus(BloodRequest.RequestStatus.ALLOCATED);
        bloodRequestRepository.save(request);

        // 5. UNITS_ALLOCATED Notification: Notify requesting hospital staff and admins
        String allocTitle = "Blood Units Allocated for Request #" + request.getId();
        String allocMsg = String.format("%d unit(s) of %s %s allocated by %s for Request #%d.",
            savedAllocations.size(), request.getBloodType().name(), request.getComponentType().getDisplayName(),
            bloodBank.getName(), request.getId());
        notificationService.notifyHospitalStaff(request.getHospital(), allocTitle, allocMsg, NotificationType.UNITS_ALLOCATED, request.getId(), "BLOOD_REQUEST");
        notificationService.notifyAdmins(allocTitle, allocMsg, NotificationType.UNITS_ALLOCATED, request.getId(), "BLOOD_REQUEST");

        return savedAllocations.stream().map(AllocationResponseDto::fromEntity).toList();
    }

    public List<AllocationResponseDto> getAllocationsForRequest(Long requestId) {
        BloodRequest request = bloodRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Blood request not found with id: " + requestId));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null ||
                !request.getHospital().getId().equals(currentUser.getAssignedHospital().getId())) {
                throw new AccessDeniedException("Access denied: You can only view allocations for your assigned hospital");
            }
        } else if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                throw new AccessDeniedException("Staff member is not an approved member of any blood bank");
            }
            Long staffBankId = currentUser.getAssignedBloodBank().getId();
            List<BloodAllocation> allocs = bloodAllocationRepository.findAllByRequestId(requestId);
            if (!allocs.isEmpty() && !allocs.get(0).getBloodBank().getId().equals(staffBankId)) {
                throw new AccessDeniedException("Access denied: You can only view allocations belonging to your assigned blood bank");
            }
        }

        return bloodAllocationRepository.findAllByRequestId(requestId).stream()
            .map(AllocationResponseDto::fromEntity)
            .toList();
    }

    /**
     * Stage 2: DISPATCH
     * Only RESERVED units can be dispatched. On successful dispatch, changes them to ISSUED.
     */
    @Transactional(rollbackFor = Exception.class)
    public DispatchResponseDto dispatchRequest(Long requestId, DispatchRequestDto dto) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != User.Role.BLOOD_BANK_STAFF && currentUser.getRole() != User.Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException("Access denied: Only blood bank staff and administrators can dispatch blood units");
        }

        BloodRequest request = bloodRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Blood request not found with id: " + requestId));

        if (request.getStatus() != BloodRequest.RequestStatus.ALLOCATED) {
            throw new IllegalArgumentException("Only ALLOCATED requests can be dispatched. Current status: " + request.getStatus());
        }

        List<BloodAllocation> allocations = bloodAllocationRepository.findAllByRequestIdAndStatus(
            requestId, BloodAllocation.AllocationStatus.ALLOCATED
        );
        if (allocations.isEmpty()) {
            throw new IllegalArgumentException("No active allocations found for request #" + requestId);
        }

        BloodBank bloodBank = allocations.get(0).getBloodBank();
        if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !bloodBank.getId().equals(currentUser.getAssignedBloodBank().getId())) {
                throw new AccessDeniedException("Access denied: You can only dispatch units from your assigned blood bank");
            }
        }

        List<Long> unitIds = allocations.stream().map(a -> a.getBloodUnit().getId()).toList();
        List<BloodUnit> lockedUnits = bloodUnitRepository.findAllByIdWithLock(unitIds);

        // Verify all units are RESERVED
        for (BloodUnit u : lockedUnits) {
            if (u.getStatus() != BloodUnit.BloodUnitStatus.RESERVED) {
                throw new IllegalArgumentException("Cannot dispatch unit " + u.getUnitId() + ": status is " + u.getStatus() + " (must be RESERVED)");
            }
            u.setStatus(BloodUnit.BloodUnitStatus.ISSUED);
            bloodUnitRepository.save(u);
        }

        // Generate unique dispatch reference: DISP-YYYYMMDD-XXXX
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randCode = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String dispatchRef = "DISP-" + dateStr + "-" + randCode;

        LocalDate dispatchDate = (dto != null && dto.getDispatchDate() != null) ? dto.getDispatchDate() : LocalDate.now();

        BloodDispatch dispatch = BloodDispatch.builder()
            .dispatchReference(dispatchRef)
            .request(request)
            .bloodBank(bloodBank)
            .hospital(request.getHospital())
            .dispatchedBy(currentUser)
            .dispatchDate(dispatchDate)
            .status(BloodDispatch.DispatchStatus.DISPATCHED)
            .notes(dto != null ? dto.getNotes() : null)
            .units(new ArrayList<>(lockedUnits))
            .build();

        BloodDispatch savedDispatch = bloodDispatchRepository.save(dispatch);

        // Update allocations status
        for (BloodAllocation a : allocations) {
            a.setStatus(BloodAllocation.AllocationStatus.DISPATCHED);
            bloodAllocationRepository.save(a);
        }

        // Update request status to DISPATCHED
        request.setStatus(BloodRequest.RequestStatus.DISPATCHED);
        bloodRequestRepository.save(request);

        // 6. UNITS_DISPATCHED Notification: Notify requesting hospital staff and admins
        String dispTitle = "Blood Units Dispatched (Ref: " + dispatchRef + ")";
        String dispMsg = String.format("Shipment %s containing %d unit(s) has been dispatched from %s to %s.",
            dispatchRef, lockedUnits.size(), bloodBank.getName(), request.getHospital().getName());
        notificationService.notifyHospitalStaff(request.getHospital(), dispTitle, dispMsg, NotificationType.UNITS_DISPATCHED, savedDispatch.getId(), "DISPATCH");
        notificationService.notifyAdmins(dispTitle, dispMsg, NotificationType.UNITS_DISPATCHED, savedDispatch.getId(), "DISPATCH");

        return DispatchResponseDto.fromEntity(savedDispatch);
    }

    /**
     * Stage 3: RECEIPT
     * Only HOSPITAL_STAFF assigned to the requesting hospital (or SYSTEM_ADMIN) can confirm receipt.
     */
    @Transactional(rollbackFor = Exception.class)
    public DispatchResponseDto confirmReceipt(Long dispatchId, ConfirmReceiptDto dto) {
        User currentUser = getCurrentUser();

        BloodDispatch dispatch = bloodDispatchRepository.findById(dispatchId)
            .orElseThrow(() -> new IllegalArgumentException("Dispatch record not found with id: " + dispatchId));

        if (dispatch.getStatus() != BloodDispatch.DispatchStatus.DISPATCHED) {
            throw new IllegalArgumentException("Only shipments with status DISPATCHED can be received. Current status: " + dispatch.getStatus());
        }

        // Strict hospital ownership check
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null ||
                !currentUser.getAssignedHospital().getId().equals(dispatch.getHospital().getId())) {
                throw new AccessDeniedException("Access denied: Only staff of the requesting hospital ("
                    + dispatch.getHospital().getName() + ") can confirm receipt");
            }
        } else if (currentUser.getRole() != User.Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException("Access denied: Blood bank staff cannot confirm receipt on behalf of the hospital");
        }

        dispatch.setStatus(BloodDispatch.DispatchStatus.DELIVERED);
        dispatch.setReceivedBy(currentUser);
        dispatch.setReceivedAt(LocalDateTime.now());
        if (dto != null && dto.getReceiptNotes() != null) {
            dispatch.setReceiptNotes(dto.getReceiptNotes());
        }
        BloodDispatch updatedDispatch = bloodDispatchRepository.save(dispatch);

        // Update allocations to COMPLETED
        List<BloodAllocation> allocations = bloodAllocationRepository.findAllByRequestId(dispatch.getRequest().getId());
        for (BloodAllocation a : allocations) {
            a.setStatus(BloodAllocation.AllocationStatus.COMPLETED);
            bloodAllocationRepository.save(a);
        }

        // Update request to FULFILLED
        BloodRequest request = dispatch.getRequest();
        request.setStatus(BloodRequest.RequestStatus.FULFILLED);
        bloodRequestRepository.save(request);

        // 7. DELIVERY_CONFIRMED Notification: Notify dispatching blood bank staff and admins
        String delivTitle = "Delivery Confirmed (Ref: " + dispatch.getDispatchReference() + ")";
        String delivMsg = String.format("%s confirmed receipt of shipment %s (Request #%d).",
            dispatch.getHospital().getName(), dispatch.getDispatchReference(), request.getId());
        notificationService.notifyBloodBankStaff(dispatch.getBloodBank(), delivTitle, delivMsg, NotificationType.DELIVERY_CONFIRMED, dispatch.getId(), "DISPATCH");
        notificationService.notifyAdmins(delivTitle, delivMsg, NotificationType.DELIVERY_CONFIRMED, dispatch.getId(), "DISPATCH");

        return DispatchResponseDto.fromEntity(updatedDispatch);
    }

    public DispatchResponseDto getDispatchByRequestId(Long requestId) {
        BloodDispatch dispatch = bloodDispatchRepository.findByRequestId(requestId)
            .orElseThrow(() -> new IllegalArgumentException("No dispatch record found for request #" + requestId));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null ||
                !currentUser.getAssignedHospital().getId().equals(dispatch.getHospital().getId())) {
                throw new AccessDeniedException("Access denied: You can only view dispatches for your assigned hospital");
            }
        } else if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !currentUser.getAssignedBloodBank().getId().equals(dispatch.getBloodBank().getId())) {
                throw new AccessDeniedException("Access denied: You can only view dispatches belonging to your assigned blood bank");
            }
        }

        return DispatchResponseDto.fromEntity(dispatch);
    }

    public DispatchResponseDto getDispatchById(Long id) {
        BloodDispatch dispatch = bloodDispatchRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Dispatch record not found with id: " + id));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null ||
                !currentUser.getAssignedHospital().getId().equals(dispatch.getHospital().getId())) {
                throw new AccessDeniedException("Access denied: You can only view dispatches for your assigned hospital");
            }
        } else if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !currentUser.getAssignedBloodBank().getId().equals(dispatch.getBloodBank().getId())) {
                throw new AccessDeniedException("Access denied: You can only view dispatches belonging to your assigned blood bank");
            }
        }

        return DispatchResponseDto.fromEntity(dispatch);
    }

    public List<DispatchResponseDto> getAllDispatches() {
        User currentUser = getCurrentUser();

        List<BloodDispatch> list;
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null) {
                return Collections.emptyList();
            }
            list = bloodDispatchRepository.findAllByHospitalIdOrderByDispatchedAtDesc(currentUser.getAssignedHospital().getId());
        } else if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                return Collections.emptyList();
            }
            list = bloodDispatchRepository.findAllByBloodBankIdOrderByDispatchedAtDesc(currentUser.getAssignedBloodBank().getId());
        } else {
            list = bloodDispatchRepository.findAllByOrderByDispatchedAtDesc();
        }

        return list.stream().map(DispatchResponseDto::fromEntity).toList();
    }
}
