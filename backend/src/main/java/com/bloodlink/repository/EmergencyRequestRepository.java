package com.bloodlink.repository;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.EmergencyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, Long> {

    List<EmergencyRequest> findByHospital(String hospital);

    List<EmergencyRequest> findByStatus(EmergencyRequest.RequestStatus status);

    List<EmergencyRequest> findByBloodType(BloodInventory.BloodType bloodType);

    List<EmergencyRequest> findByHospitalAndStatus(String hospital, EmergencyRequest.RequestStatus status);
}
