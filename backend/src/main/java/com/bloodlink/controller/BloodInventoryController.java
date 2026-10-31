package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.model.BloodInventory;
import com.bloodlink.service.BloodInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class BloodInventoryController {

    private final BloodInventoryService bloodInventoryService;

    /** GET /api/inventory — list all inventory records */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BloodInventory>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(bloodInventoryService.getAllInventory()));
    }

    /** GET /api/inventory/{id} — get a single inventory record */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BloodInventory>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodInventoryService.getInventoryById(id)));
    }

    /** GET /api/inventory/hospital/{hospital} — filter by hospital */
    @GetMapping("/hospital/{hospital}")
    public ResponseEntity<ApiResponse<List<BloodInventory>>> getByHospital(@PathVariable String hospital) {
        return ResponseEntity.ok(ApiResponse.ok(bloodInventoryService.getInventoryByHospital(hospital)));
    }

    /** GET /api/inventory/low-stock?threshold=500 — get low-stock items */
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<BloodInventory>>> getLowStock(
            @RequestParam(defaultValue = "500") int threshold) {
        return ResponseEntity.ok(ApiResponse.ok(bloodInventoryService.getLowStockInventory(threshold)));
    }

    /** POST /api/inventory — create a new inventory record */
    @PostMapping
    public ResponseEntity<ApiResponse<BloodInventory>> create(@RequestBody BloodInventory inventory) {
        BloodInventory created = bloodInventoryService.createInventory(inventory);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Inventory created", created));
    }

    /** PUT /api/inventory/{id} — update an existing inventory record */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BloodInventory>> update(
            @PathVariable Long id,
            @RequestBody BloodInventory inventory) {
        return ResponseEntity.ok(ApiResponse.ok("Inventory updated", bloodInventoryService.updateInventory(id, inventory)));
    }

    /** PATCH /api/inventory/{id}/adjust?delta=100 — adjust quantity by delta */
    @PatchMapping("/{id}/adjust")
    public ResponseEntity<ApiResponse<BloodInventory>> adjust(
            @PathVariable Long id,
            @RequestParam int delta) {
        return ResponseEntity.ok(ApiResponse.ok("Quantity adjusted", bloodInventoryService.adjustQuantity(id, delta)));
    }

    /** DELETE /api/inventory/{id} — delete an inventory record */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bloodInventoryService.deleteInventory(id);
        return ResponseEntity.ok(ApiResponse.ok("Inventory record deleted", null));
    }
}
