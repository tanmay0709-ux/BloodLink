package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.DashboardAnalyticsDto;
import com.bloodlink.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * GET /api/analytics/dashboard
     * Returns comprehensive, role-scoped analytics for the authenticated user.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardAnalyticsDto>> getDashboardAnalytics() {
        DashboardAnalyticsDto data = analyticsService.getDashboardAnalytics();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
