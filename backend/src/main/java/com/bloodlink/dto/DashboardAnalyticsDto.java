package com.bloodlink.dto;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.BloodRequest;
import com.bloodlink.model.BloodUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsDto {

    private String role;
    private String username;
    private Long facilityId;
    private String facilityName;
    private String facilityType; // HOSPITAL, BLOOD_BANK, or ALL (for admin)

    // 1. Overview counts
    private OverviewStats overview;

    // 2. Inventory Analytics (Blood Bank & Admin only)
    private InventoryAnalytics inventoryAnalytics;

    // 3. Request Analytics (All roles, scoped accordingly)
    private RequestAnalytics requestAnalytics;

    // 4. Donation Analytics (Blood Bank & Admin only)
    private DonationAnalytics donationAnalytics;

    // 5. Operational / Dispatch Analytics (All roles, scoped accordingly)
    private OperationalAnalytics operationalAnalytics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewStats {
        private Long totalDonors;
        private Long totalUnits;
        private Long availableUnits;
        private Long reservedUnits;
        private Long issuedUnits;
        private Long expiredUnits;
        private Long discardedUnits;
        private Long quarantinedUnits;
        private Long totalRequests;
        private Long pendingRequests;
        private Long reviewedRequests;
        private Long allocatedRequests;
        private Long dispatchedRequests;
        private Long fulfilledRequests;
        private Long cancelledRequests;
        private Long rejectedRequests;
        private Long totalDonations;
        private Long activeDispatches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryAnalytics {
        private List<BloodGroupCount> byBloodGroup;
        private List<ComponentCount> byComponentType;
        private List<UnitNearExpiryDto> nearExpiryUnits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BloodGroupCount {
        private BloodInventory.BloodType bloodType;
        private Long count; // individual bags count
        private Long totalVolumeMl; // volume in ml
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentCount {
        private BloodUnit.BloodComponentType componentType;
        private String displayName;
        private Long count;
        private Long totalVolumeMl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitNearExpiryDto {
        private Long id;
        private String unitId;
        private BloodInventory.BloodType bloodType;
        private BloodUnit.BloodComponentType componentType;
        private Integer quantity; // ml
        private LocalDate expiryDate;
        private Long daysRemaining;
        private String bloodBankName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestAnalytics {
        private List<StatusCount> byStatus;
        private List<PriorityCount> byPriority;
        private List<RecentRequestDto> recentRequests;
        private Double fulfillmentRatePercent;
        private Long eligibleRequestsCount;
        private Long fulfilledRequestsCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCount {
        private BloodRequest.RequestStatus status;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriorityCount {
        private BloodRequest.RequestPriority priority;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentRequestDto {
        private Long id;
        private String hospitalName;
        private BloodInventory.BloodType bloodType;
        private BloodUnit.BloodComponentType componentType;
        private Integer quantity; // units count
        private BloodRequest.RequestPriority priority;
        private BloodRequest.RequestStatus status;
        private LocalDate requiredDate;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonationAnalytics {
        private Long totalCompletedDonations;
        private Long totalDonatedVolumeMl;
        private List<MonthlyDonationTrend> monthlyTrends;
        private List<DonationByGroup> byBloodGroup;
        private List<RecentDonationDto> recentDonations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyDonationTrend {
        private String yearMonth; // "YYYY-MM"
        private String label;     // "Nov 2026"
        private Long donationCount;
        private Long totalVolumeMl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonationByGroup {
        private BloodInventory.BloodType bloodType;
        private Long count;
        private Long totalVolumeMl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentDonationDto {
        private Long id;
        private String donorName;
        private BloodInventory.BloodType bloodType;
        private Integer quantity; // ml
        private LocalDate donationDate;
        private String status;
        private String bloodBankName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationalAnalytics {
        private Long totalDispatches;
        private Long activeInTransit;
        private Long deliveredCount;
        private List<RecentDispatchDto> recentDispatches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentDispatchDto {
        private Long id;
        private String dispatchReference;
        private String hospitalName;
        private String bloodBankName;
        private LocalDate dispatchDate;
        private String status;
        private Integer unitsCount;
    }
}
