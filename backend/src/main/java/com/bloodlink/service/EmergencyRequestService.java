package com.bloodlink.service;

import com.bloodlink.model.EmergencyRequest;
import com.bloodlink.model.User;
import com.bloodlink.repository.EmergencyRequestRepository;
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
public class EmergencyRequestService {

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("User not found: " + auth.getName()));
    }

    public List<EmergencyRequest> getAllRequests() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                return List.of();
            }
            return emergencyRequestRepository.findByHospital(currentUser.getAssignedHospital().getName());
        }
        return emergencyRequestRepository.findAll();
    }

    public EmergencyRequest getRequestById(Long id) {
        User currentUser = getCurrentUser();
        EmergencyRequest request = emergencyRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Emergency request not found with id: " + id));

        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !request.getHospital().equals(currentUser.getAssignedHospital().getName())) {
                throw new AccessDeniedException("Access denied: You can only view requests from your assigned hospital");
            }
        }
        return request;
    }

    public List<EmergencyRequest> getRequestsByHospital(String hospital) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !currentUser.getAssignedHospital().getName().equalsIgnoreCase(hospital)) {
                throw new AccessDeniedException("Access denied: You can only view requests from your assigned hospital");
            }
        }
        return emergencyRequestRepository.findByHospital(hospital);
    }

    public List<EmergencyRequest> getRequestsByStatus(EmergencyRequest.RequestStatus status) {
        return emergencyRequestRepository.findByStatus(status);
    }

    @Transactional
    public EmergencyRequest createRequest(EmergencyRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                throw new AccessDeniedException("Staff member is not an approved member of any hospital");
            }
            // Enforce the authenticated user's assigned hospital. Ignore any hospital identity supplied by the client.
            request.setHospital(currentUser.getAssignedHospital().getName());
        } else if (currentUser.getRole() == User.Role.SYSTEM_ADMIN) {
            if (request.getHospital() == null || request.getHospital().isBlank()) {
                throw new IllegalArgumentException("Hospital name is required");
            }
        } else {
            throw new AccessDeniedException("Access denied: Only hospital staff and administrators can create emergency requests");
        }
        request.setStatus(EmergencyRequest.RequestStatus.PENDING);
        return emergencyRequestRepository.save(request);
    }

    @Transactional
    public EmergencyRequest updateStatus(Long id, EmergencyRequest.RequestStatus newStatus) {
        EmergencyRequest request = getRequestById(id);
        request.setStatus(newStatus);
        return emergencyRequestRepository.save(request);
    }

    @Transactional
    public EmergencyRequest updateRequest(Long id, EmergencyRequest updated) {
        User currentUser = getCurrentUser();
        EmergencyRequest existing = getRequestById(id);
        if (currentUser.getRole() == User.Role.HOSPITAL_STAFF) {
            if (currentUser.getAssignedHospital() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED
                    || !existing.getHospital().equals(currentUser.getAssignedHospital().getName())) {
                throw new AccessDeniedException("Access denied: You can only update requests belonging to your assigned hospital");
            }
            existing.setBloodType(updated.getBloodType());
            existing.setQuantity(updated.getQuantity());
            existing.setNotes(updated.getNotes());
            return emergencyRequestRepository.save(existing);
        } else if (currentUser.getRole() == User.Role.SYSTEM_ADMIN) {
            existing.setBloodType(updated.getBloodType());
            existing.setQuantity(updated.getQuantity());
            if (updated.getHospital() != null && !updated.getHospital().isBlank()) {
                existing.setHospital(updated.getHospital());
            }
            existing.setNotes(updated.getNotes());
            return emergencyRequestRepository.save(existing);
        } else {
            throw new AccessDeniedException("Access denied");
        }
    }

    @Transactional
    public void deleteRequest(Long id) {
        emergencyRequestRepository.deleteById(id);
    }
}
