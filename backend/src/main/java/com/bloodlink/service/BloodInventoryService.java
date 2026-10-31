package com.bloodlink.service;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.repository.BloodInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BloodInventoryService {

    private final BloodInventoryRepository bloodInventoryRepository;

    public List<BloodInventory> getAllInventory() {
        return bloodInventoryRepository.findAll();
    }

    public BloodInventory getInventoryById(Long id) {
        return bloodInventoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Blood inventory record not found with id: " + id));
    }

    public List<BloodInventory> getInventoryByHospital(String hospital) {
        return bloodInventoryRepository.findByHospital(hospital);
    }

    public List<BloodInventory> getInventoryByBloodType(BloodInventory.BloodType bloodType) {
        return bloodInventoryRepository.findByBloodType(bloodType);
    }

    public List<BloodInventory> getLowStockInventory(int threshold) {
        return bloodInventoryRepository.findLowStock(threshold);
    }

    @Transactional
    public BloodInventory createInventory(BloodInventory inventory) {
        return bloodInventoryRepository.save(inventory);
    }

    @Transactional
    public BloodInventory updateInventory(Long id, BloodInventory updated) {
        BloodInventory existing = getInventoryById(id);
        existing.setBloodType(updated.getBloodType());
        existing.setQuantity(updated.getQuantity());
        existing.setHospital(updated.getHospital());
        return bloodInventoryRepository.save(existing);
    }

    @Transactional
    public void deleteInventory(Long id) {
        bloodInventoryRepository.deleteById(id);
    }

    @Transactional
    public BloodInventory adjustQuantity(Long id, int delta) {
        BloodInventory inventory = getInventoryById(id);
        int newQty = inventory.getQuantity() + delta;
        if (newQty < 0) {
            throw new IllegalStateException("Insufficient stock: cannot reduce below 0");
        }
        inventory.setQuantity(newQty);
        return bloodInventoryRepository.save(inventory);
    }
}
