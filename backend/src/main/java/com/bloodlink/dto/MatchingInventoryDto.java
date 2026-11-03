package com.bloodlink.dto;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.BloodUnit;

import java.util.List;

public class MatchingInventoryDto {

    private Long requestId;
    private BloodInventory.BloodType recipientBloodType;
    private BloodUnit.BloodComponentType componentType;
    private List<BloodInventory.BloodType> compatibleBloodTypes;
    private String disclaimer;
    private List<BloodUnitResponse> matchingUnits;
    private int totalAvailableUnits;

    public MatchingInventoryDto() {}

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public BloodInventory.BloodType getRecipientBloodType() { return recipientBloodType; }
    public void setRecipientBloodType(BloodInventory.BloodType recipientBloodType) { this.recipientBloodType = recipientBloodType; }

    public BloodUnit.BloodComponentType getComponentType() { return componentType; }
    public void setComponentType(BloodUnit.BloodComponentType componentType) { this.componentType = componentType; }

    public List<BloodInventory.BloodType> getCompatibleBloodTypes() { return compatibleBloodTypes; }
    public void setCompatibleBloodTypes(List<BloodInventory.BloodType> compatibleBloodTypes) { this.compatibleBloodTypes = compatibleBloodTypes; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public List<BloodUnitResponse> getMatchingUnits() { return matchingUnits; }
    public void setMatchingUnits(List<BloodUnitResponse> matchingUnits) { this.matchingUnits = matchingUnits; }

    public int getTotalAvailableUnits() { return totalAvailableUnits; }
    public void setTotalAvailableUnits(int totalAvailableUnits) { this.totalAvailableUnits = totalAvailableUnits; }
}
