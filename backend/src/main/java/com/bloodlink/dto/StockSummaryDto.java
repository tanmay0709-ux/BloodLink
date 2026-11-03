package com.bloodlink.dto;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.BloodUnit;

import java.util.Map;

public class StockSummaryDto {

    private long totalUnits;
    private long availableUnits;
    private long reservedUnits;
    private long issuedUnits;
    private long expiredUnits;
    private long discardedUnits;
    private long quarantinedUnits;

    // Available units count per blood type
    private Map<BloodInventory.BloodType, Long> availableByBloodType;

    // Total quantity in ml per blood type
    private Map<BloodInventory.BloodType, Long> quantityMlByBloodType;

    public StockSummaryDto() {}

    public long getTotalUnits() { return totalUnits; }
    public void setTotalUnits(long totalUnits) { this.totalUnits = totalUnits; }

    public long getAvailableUnits() { return availableUnits; }
    public void setAvailableUnits(long availableUnits) { this.availableUnits = availableUnits; }

    public long getReservedUnits() { return reservedUnits; }
    public void setReservedUnits(long reservedUnits) { this.reservedUnits = reservedUnits; }

    public long getIssuedUnits() { return issuedUnits; }
    public void setIssuedUnits(long issuedUnits) { this.issuedUnits = issuedUnits; }

    public long getExpiredUnits() { return expiredUnits; }
    public void setExpiredUnits(long expiredUnits) { this.expiredUnits = expiredUnits; }

    public long getDiscardedUnits() { return discardedUnits; }
    public void setDiscardedUnits(long discardedUnits) { this.discardedUnits = discardedUnits; }

    public long getQuarantinedUnits() { return quarantinedUnits; }
    public void setQuarantinedUnits(long quarantinedUnits) { this.quarantinedUnits = quarantinedUnits; }

    public Map<BloodInventory.BloodType, Long> getAvailableByBloodType() { return availableByBloodType; }
    public void setAvailableByBloodType(Map<BloodInventory.BloodType, Long> availableByBloodType) { this.availableByBloodType = availableByBloodType; }

    public Map<BloodInventory.BloodType, Long> getQuantityMlByBloodType() { return quantityMlByBloodType; }
    public void setQuantityMlByBloodType(Map<BloodInventory.BloodType, Long> quantityMlByBloodType) { this.quantityMlByBloodType = quantityMlByBloodType; }
}
