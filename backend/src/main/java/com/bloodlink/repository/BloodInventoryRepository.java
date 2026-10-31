package com.bloodlink.repository;

import com.bloodlink.model.BloodInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {

    List<BloodInventory> findByHospital(String hospital);

    List<BloodInventory> findByBloodType(BloodInventory.BloodType bloodType);

    Optional<BloodInventory> findByHospitalAndBloodType(String hospital, BloodInventory.BloodType bloodType);

    @Query("SELECT b FROM BloodInventory b WHERE b.quantity < :threshold")
    List<BloodInventory> findLowStock(int threshold);
}
