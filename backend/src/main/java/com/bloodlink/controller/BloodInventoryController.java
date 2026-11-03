package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.model.BloodInventory;
import com.bloodlink.service.BloodInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class BloodInventoryController {

    private final BloodInventoryService bloodInventoryService;

    /** All authenticated roles can read inventory */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BloodInventory>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(bloodInventoryService.getAllInventory()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BloodInventory>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodInventoryService.getInventoryById(id)));
    }

    @GetMapping("/hospital/{hospital}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BloodInventory>>> getByHospital(@PathVariable String hospital) {
        return ResponseEntity.ok(ApiResponse.ok(bloodInventoryService.getInventoryByHospital(hospital)));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BloodInventory>>> getLowStock(
            @RequestParam(defaultValue = "500") int threshold) {
        return ResponseEntity.ok(ApiResponse.ok(bloodInventoryService.getLowStockInventory(threshold)));
    }

    /** Only BLOOD_BANK_STAFF and SYSTEM_ADMIN can create/update/delete/adjust */
    @PostMapping
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<BloodInventory>> create(@RequestBody BloodInventory inventory) {
        BloodInventory created = bloodInventoryService.createInventory(inventory);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Inventory created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<BloodInventory>> update(
            @PathVariable Long id, @RequestBody BloodInventory inventory) {
        return ResponseEntity.ok(ApiResponse.ok("Inventory updated",
            bloodInventoryService.updateInventory(id, inventory)));
    }

    @PatchMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<BloodInventory>> adjust(
            @PathVariable Long id, @RequestParam int delta) {
        return ResponseEntity.ok(ApiResponse.ok("Quantity adjusted",
            bloodInventoryService.adjustQuantity(id, delta)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bloodInventoryService.deleteInventory(id);
        return ResponseEntity.ok(ApiResponse.ok("Inventory record deleted", null));
    }
}
