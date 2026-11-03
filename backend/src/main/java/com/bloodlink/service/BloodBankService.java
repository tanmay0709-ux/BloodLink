package com.bloodlink.service;

import com.bloodlink.dto.BloodBankRequest;
import com.bloodlink.dto.UserSummaryDto;
import com.bloodlink.model.BloodBank;
import com.bloodlink.model.User;
import com.bloodlink.repository.BloodBankRepository;
import com.bloodlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BloodBankService {

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

    public List<BloodBank> getAll() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.SYSTEM_ADMIN) {
            return bloodBankRepository.findAll();
        } else if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                return List.of();
            }
            return List.of(currentUser.getAssignedBloodBank());
        } else {
            throw new AccessDeniedException("Access denied: Hospital staff cannot access blood banks");
        }
    }

    public BloodBank getById(Long id) {
        User currentUser = getCurrentUser();
        BloodBank bloodBank = bloodBankRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + id));

        if (currentUser.getRole() == User.Role.SYSTEM_ADMIN) {
            return bloodBank;
        } else if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !currentUser.getAssignedBloodBank().getId().equals(id)) {
                throw new AccessDeniedException("Access denied: You can only access your assigned blood bank");
            }
            return bloodBank;
        } else {
            throw new AccessDeniedException("Access denied: Hospital staff cannot access blood banks");
        }
    }

    @Transactional
    public BloodBank create(BloodBankRequest request) {
        if (bloodBankRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A blood bank with name '" + request.getName() + "' already exists");
        }
        BloodBank bloodBank = BloodBank.builder()
            .name(request.getName())
            .address(request.getAddress())
            .contactPhone(request.getContactPhone())
            .active(true)
            .build();
        return bloodBankRepository.save(bloodBank);
    }

    @Transactional
    public BloodBank update(Long id, BloodBankRequest request) {
        User currentUser = getCurrentUser();
        BloodBank bloodBank = bloodBankRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + id));

        if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !currentUser.getAssignedBloodBank().getId().equals(id)) {
                throw new AccessDeniedException("Access denied: You can only update your assigned blood bank");
            }
        } else if (currentUser.getRole() != User.Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException("Access denied: Hospital staff cannot update blood banks");
        }

        if (!bloodBank.getName().equals(request.getName()) && bloodBankRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A blood bank with name '" + request.getName() + "' already exists");
        }
        bloodBank.setName(request.getName());
        bloodBank.setAddress(request.getAddress());
        bloodBank.setContactPhone(request.getContactPhone());
        return bloodBankRepository.save(bloodBank);
    }

    @Transactional
    public void delete(Long id) {
        BloodBank bloodBank = bloodBankRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + id));
        bloodBank.setActive(false);
        bloodBankRepository.save(bloodBank);
    }

    public List<UserSummaryDto> getStaff(Long bloodBankId) {
        bloodBankRepository.findById(bloodBankId)
            .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + bloodBankId));
        return userRepository.findAll().stream()
            .filter(u -> u.getAssignedBloodBank() != null && u.getAssignedBloodBank().getId().equals(bloodBankId))
            .map(this::toSummaryDto)
            .toList();
    }

    public List<UserSummaryDto> getPendingStaff(Long bloodBankId) {
        bloodBankRepository.findById(bloodBankId)
            .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + bloodBankId));
        return userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.Role.BLOOD_BANK_STAFF
                && u.getPendingBloodBank() != null
                && u.getPendingBloodBank().getId().equals(bloodBankId)
                && u.getApprovalStatus() == User.ApprovalStatus.PENDING)
            .map(this::toSummaryDto)
            .toList();
    }

    @Transactional
    public UserSummaryDto approveStaff(Long bloodBankId, Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Users cannot approve their own blood bank association");
        }
        BloodBank bloodBank = bloodBankRepository.findById(bloodBankId)
            .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + bloodBankId));
        if (!bloodBank.getActive()) {
            throw new IllegalArgumentException("Cannot approve staff for an inactive blood bank");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (user.getRole() != User.Role.BLOOD_BANK_STAFF) {
            throw new IllegalArgumentException("Only BLOOD_BANK_STAFF users can be approved for blood bank association");
        }
        if (user.getPendingBloodBank() == null || !user.getPendingBloodBank().getId().equals(bloodBankId)) {
            throw new IllegalArgumentException("User does not have a pending request for blood bank with id: " + bloodBankId);
        }

        user.setAssignedBloodBank(bloodBank);
        user.setPendingBloodBank(null);
        user.setApprovalStatus(User.ApprovalStatus.APPROVED);
        userRepository.save(user);
        return toSummaryDto(user);
    }

    @Transactional
    public void rejectStaff(Long bloodBankId, Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Users cannot reject their own blood bank association");
        }
        bloodBankRepository.findById(bloodBankId)
            .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + bloodBankId));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (user.getRole() != User.Role.BLOOD_BANK_STAFF) {
            throw new IllegalArgumentException("Only BLOOD_BANK_STAFF users can be rejected for blood bank association");
        }
        if (user.getPendingBloodBank() == null || !user.getPendingBloodBank().getId().equals(bloodBankId)) {
            throw new IllegalArgumentException("User does not have a pending request for blood bank with id: " + bloodBankId);
        }

        user.setAssignedBloodBank(null);
        user.setPendingBloodBank(null);
        user.setApprovalStatus(User.ApprovalStatus.REJECTED);
        userRepository.save(user);
    }

    @Transactional
    public UserSummaryDto assignStaff(Long bloodBankId, Long userId) {
        BloodBank bloodBank = bloodBankRepository.findById(bloodBankId)
            .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + bloodBankId));
        if (!bloodBank.getActive()) {
            throw new IllegalArgumentException("Cannot assign staff to an inactive blood bank");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (user.getRole() != User.Role.BLOOD_BANK_STAFF) {
            throw new IllegalArgumentException("Only BLOOD_BANK_STAFF users can be assigned to a blood bank");
        }
        user.setAssignedBloodBank(bloodBank);
        user.setPendingBloodBank(null);
        user.setApprovalStatus(User.ApprovalStatus.APPROVED);
        userRepository.save(user);
        return toSummaryDto(user);
    }

    @Transactional
    public void unassignStaff(Long bloodBankId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (user.getAssignedBloodBank() == null || !user.getAssignedBloodBank().getId().equals(bloodBankId)) {
            throw new IllegalArgumentException("User is not assigned to blood bank with id: " + bloodBankId);
        }
        user.setAssignedBloodBank(null);
        user.setApprovalStatus(null);
        userRepository.save(user);
    }

    private UserSummaryDto toSummaryDto(User u) {
        Long hId = u.getAssignedHospital() != null ? u.getAssignedHospital().getId() : null;
        String hName = u.getAssignedHospital() != null ? u.getAssignedHospital().getName() : null;
        Long pHId = u.getPendingHospital() != null ? u.getPendingHospital().getId() : null;
        String pHName = u.getPendingHospital() != null ? u.getPendingHospital().getName() : null;
        String approvalStatus = u.getApprovalStatus() != null ? u.getApprovalStatus().name()
            : ((u.getAssignedHospital() != null || u.getAssignedBloodBank() != null) ? "APPROVED" : null);
        Long bId = u.getAssignedBloodBank() != null ? u.getAssignedBloodBank().getId() : null;
        String bName = u.getAssignedBloodBank() != null ? u.getAssignedBloodBank().getName() : null;
        Long pBId = u.getPendingBloodBank() != null ? u.getPendingBloodBank().getId() : null;
        String pBName = u.getPendingBloodBank() != null ? u.getPendingBloodBank().getName() : null;
        return new UserSummaryDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(),
            hId, hName, pHId, pHName, approvalStatus, bId, bName, pBId, pBName);
    }
}
