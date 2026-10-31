package com.bloodlink.service;

import com.bloodlink.model.EmergencyRequest;
import com.bloodlink.repository.EmergencyRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmergencyRequestService {

    private final EmergencyRequestRepository emergencyRequestRepository;

    public List<EmergencyRequest> getAllRequests() {
        return emergencyRequestRepository.findAll();
    }

    public EmergencyRequest getRequestById(Long id) {
        return emergencyRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Emergency request not found with id: " + id));
    }

    public List<EmergencyRequest> getRequestsByHospital(String hospital) {
        return emergencyRequestRepository.findByHospital(hospital);
    }

    public List<EmergencyRequest> getRequestsByStatus(EmergencyRequest.RequestStatus status) {
        return emergencyRequestRepository.findByStatus(status);
    }

    @Transactional
    public EmergencyRequest createRequest(EmergencyRequest request) {
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
        EmergencyRequest existing = getRequestById(id);
        existing.setBloodType(updated.getBloodType());
        existing.setQuantity(updated.getQuantity());
        existing.setHospital(updated.getHospital());
        existing.setNotes(updated.getNotes());
        return emergencyRequestRepository.save(existing);
    }

    @Transactional
    public void deleteRequest(Long id) {
        emergencyRequestRepository.deleteById(id);
    }
}
