package com.bloodlink.repository;

import com.bloodlink.model.BloodAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodAllocationRepository extends JpaRepository<BloodAllocation, Long> {

    List<BloodAllocation> findAllByRequestId(Long requestId);

    List<BloodAllocation> findAllByRequestIdAndStatus(Long requestId, BloodAllocation.AllocationStatus status);

    List<BloodAllocation> findAllByBloodBankId(Long bloodBankId);

    boolean existsByRequestIdAndStatus(Long requestId, BloodAllocation.AllocationStatus status);

    boolean existsByBloodUnitIdAndStatus(Long bloodUnitId, BloodAllocation.AllocationStatus status);
}
