package com.bloodlink.service;

import com.bloodlink.dto.HospitalRequest;
import com.bloodlink.dto.UserSummaryDto;
import com.bloodlink.model.Hospital;
import com.bloodlink.model.User;
import com.bloodlink.repository.HospitalRepository;
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
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("User not found: " + auth.getName()));
    }

    public List<Hospital> getAll() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.SYSTEM_ADMIN) {
            return hospitalRepository.findAll();
        } else if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                return List.of();
            }
            return List.of(currentUser.getAssignedHospital());
        } else {
            throw new AccessDeniedException("Access denied: Blood bank staff cannot access hospitals");
        }
    }

    public Hospital getById(Long id) {
        User currentUser = getCurrentUser();
        Hospital hospital = hospitalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Hospital not found with id: " + id));

        if (currentUser.getRole() == User.Role.SYSTEM_ADMIN) {
            return hospital;
        } else if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !currentUser.getAssignedHospital().getId().equals(id)) {
                throw new AccessDeniedException("Access denied: You can only access your assigned hospital");
            }
            return hospital;
        } else {
            throw new AccessDeniedException("Access denied: Blood bank staff cannot access hospitals");
        }
    }

    @Transactional
    public Hospital create(HospitalRequest request) {
        if (hospitalRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A hospital with name '" + request.getName() + "' already exists");
        }
        Hospital hospital = Hospital.builder()
            .name(request.getName())
            .address(request.getAddress())
            .contactPhone(request.getContactPhone())
            .active(true)
            .build();
        return hospitalRepository.save(hospital);
    }

    @Transactional
    public Hospital update(Long id, HospitalRequest request) {
        User currentUser = getCurrentUser();
        Hospital hospital = hospitalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Hospital not found with id: " + id));

        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !currentUser.getAssignedHospital().getId().equals(id)) {
                throw new AccessDeniedException("Access denied: You can only update your assigned hospital");
            }
        } else if (currentUser.getRole() != User.Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException("Access denied: Blood bank staff cannot update hospitals");
        }

        if (!hospital.getName().equals(request.getName()) && hospitalRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A hospital with name '" + request.getName() + "' already exists");
        }
        hospital.setName(request.getName());
        hospital.setAddress(request.getAddress());
        hospital.setContactPhone(request.getContactPhone());
        return hospitalRepository.save(hospital);
    }

    @Transactional
    public void delete(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Hospital not found with id: " + id));
        hospital.setActive(false);
        hospitalRepository.save(hospital);
    }

    public List<UserSummaryDto> getStaff(Long hospitalId) {
        hospitalRepository.findById(hospitalId)
            .orElseThrow(() -> new IllegalArgumentException("Hospital not found with id: " + hospitalId));
        return userRepository.findAll().stream()
            .filter(u -> u.getAssignedHospital() != null && u.getAssignedHospital().getId().equals(hospitalId))
            .map(this::toSummaryDto)
            .toList();
    }

    public List<UserSummaryDto> getPendingStaff(Long hospitalId) {
        hospitalRepository.findById(hospitalId)
            .orElseThrow(() -> new IllegalArgumentException("Hospital not found with id: " + hospitalId));
        return userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.Role.HOSPITAL_STAFF
                && u.getPendingHospital() != null
                && u.getPendingHospital().getId().equals(hospitalId)
                && u.getApprovalStatus() == User.ApprovalStatus.PENDING)
            .map(this::toSummaryDto)
            .toList();
    }

    @Transactional
    public UserSummaryDto approveStaff(Long hospitalId, Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Users cannot approve their own hospital association");
        }
        Hospital hospital = hospitalRepository.findById(hospitalId)
            .orElseThrow(() -> new IllegalArgumentException("Hospital not found with id: " + hospitalId));
        if (!hospital.getActive()) {
            throw new IllegalArgumentException("Cannot approve staff for an inactive hospital");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (user.getRole() != User.Role.HOSPITAL_STAFF) {
            throw new IllegalArgumentException("Only HOSPITAL_STAFF users can be approved for hospital association");
        }
        if (user.getPendingHospital() == null || !user.getPendingHospital().getId().equals(hospitalId)) {
            throw new IllegalArgumentException("User does not have a pending request for hospital with id: " + hospitalId);
        }

        user.setAssignedHospital(hospital);
        user.setPendingHospital(null);
        user.setApprovalStatus(User.ApprovalStatus.APPROVED);
        userRepository.save(user);
        return toSummaryDto(user);
    }

    @Transactional
    public void rejectStaff(Long hospitalId, Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Users cannot reject their own hospital association");
        }
        hospitalRepository.findById(hospitalId)
            .orElseThrow(() -> new IllegalArgumentException("Hospital not found with id: " + hospitalId));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (user.getRole() != User.Role.HOSPITAL_STAFF) {
            throw new IllegalArgumentException("Only HOSPITAL_STAFF users can be rejected for hospital association");
        }
        if (user.getPendingHospital() == null || !user.getPendingHospital().getId().equals(hospitalId)) {
            throw new IllegalArgumentException("User does not have a pending request for hospital with id: " + hospitalId);
        }

        user.setAssignedHospital(null);
        user.setPendingHospital(null);
        user.setApprovalStatus(User.ApprovalStatus.REJECTED);
        userRepository.save(user);
    }

    @Transactional
    public UserSummaryDto assignStaff(Long hospitalId, Long userId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
            .orElseThrow(() -> new IllegalArgumentException("Hospital not found with id: " + hospitalId));
        if (!hospital.getActive()) {
            throw new IllegalArgumentException("Cannot assign staff to an inactive hospital");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (user.getRole() != User.Role.HOSPITAL_STAFF) {
            throw new IllegalArgumentException("Only HOSPITAL_STAFF users can be assigned to a hospital");
        }
        user.setAssignedHospital(hospital);
        user.setPendingHospital(null);
        user.setApprovalStatus(User.ApprovalStatus.APPROVED);
        userRepository.save(user);
        return toSummaryDto(user);
    }

    @Transactional
    public void unassignStaff(Long hospitalId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (user.getAssignedHospital() == null || !user.getAssignedHospital().getId().equals(hospitalId)) {
            throw new IllegalArgumentException("User is not assigned to hospital with id: " + hospitalId);
        }
        user.setAssignedHospital(null);
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
