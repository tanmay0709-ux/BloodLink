package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.model.EmergencyRequest;
import com.bloodlink.service.EmergencyRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
public class EmergencyRequestController {

    private final EmergencyRequestService emergencyRequestService;

    /** GET /api/emergency — list all emergency requests */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmergencyRequest>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(emergencyRequestService.getAllRequests()));
    }

    /** GET /api/emergency/{id} — get a single request */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmergencyRequest>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(emergencyRequestService.getRequestById(id)));
    }

    /** GET /api/emergency/hospital/{hospital} — filter by hospital */
    @GetMapping("/hospital/{hospital}")
    public ResponseEntity<ApiResponse<List<EmergencyRequest>>> getByHospital(@PathVariable String hospital) {
        return ResponseEntity.ok(ApiResponse.ok(emergencyRequestService.getRequestsByHospital(hospital)));
    }

    /** GET /api/emergency/status/{status} — filter by status */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<EmergencyRequest>>> getByStatus(
            @PathVariable EmergencyRequest.RequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(emergencyRequestService.getRequestsByStatus(status)));
    }

    /** POST /api/emergency — create a new emergency request */
    @PostMapping
    public ResponseEntity<ApiResponse<EmergencyRequest>> create(@RequestBody EmergencyRequest request) {
        EmergencyRequest created = emergencyRequestService.createRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Emergency request created", created));
    }

    /** PATCH /api/emergency/{id}/status?status=APPROVED — update request status */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<EmergencyRequest>> updateStatus(
            @PathVariable Long id,
            @RequestParam EmergencyRequest.RequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated",
            emergencyRequestService.updateStatus(id, status)));
    }

    /** PUT /api/emergency/{id} — update a request */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmergencyRequest>> update(
            @PathVariable Long id,
            @RequestBody EmergencyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Request updated",
            emergencyRequestService.updateRequest(id, request)));
    }

    /** DELETE /api/emergency/{id} — delete a request */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        emergencyRequestService.deleteRequest(id);
        return ResponseEntity.ok(ApiResponse.ok("Emergency request deleted", null));
    }
}
