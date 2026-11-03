package com.bloodlink.service;

import com.bloodlink.model.BloodInventory.BloodType;
import com.bloodlink.model.BloodUnit.BloodComponentType;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class BloodCompatibilityService {

    public static final String CLINICAL_DISCLAIMER =
        "EDUCATIONAL NOTICE: ABO/Rh compatibility filtering is provided for educational and inventory screening purposes only. " +
        "Actual blood component transfusions strictly require laboratory confirmation, antibody screening, compatibility crossmatching, " +
        "and direct clinical oversight by licensed medical staff.";

    /**
     * Returns the list of donor blood types that are compatible with the recipient.
     * For RED_BLOOD_CELLS: applies standard ABO/Rh compatibility rules.
     * For all other components: strictly requires exact ABO/Rh blood type match.
     */
    public List<BloodType> getCompatibleDonorBloodTypes(BloodType recipientType, BloodComponentType componentType) {
        if (recipientType == null) {
            return Collections.emptyList();
        }

        // Apply compatibility filtering ONLY to RED_BLOOD_CELLS
        if (componentType == BloodComponentType.RED_BLOOD_CELLS) {
            return switch (recipientType) {
                case O_NEGATIVE -> List.of(BloodType.O_NEGATIVE);
                case O_POSITIVE -> List.of(BloodType.O_POSITIVE, BloodType.O_NEGATIVE);
                case A_NEGATIVE -> List.of(BloodType.A_NEGATIVE, BloodType.O_NEGATIVE);
                case A_POSITIVE -> List.of(BloodType.A_POSITIVE, BloodType.A_NEGATIVE, BloodType.O_POSITIVE, BloodType.O_NEGATIVE);
                case B_NEGATIVE -> List.of(BloodType.B_NEGATIVE, BloodType.O_NEGATIVE);
                case B_POSITIVE -> List.of(BloodType.B_POSITIVE, BloodType.B_NEGATIVE, BloodType.O_POSITIVE, BloodType.O_NEGATIVE);
                case AB_NEGATIVE -> List.of(BloodType.AB_NEGATIVE, BloodType.A_NEGATIVE, BloodType.B_NEGATIVE, BloodType.O_NEGATIVE);
                case AB_POSITIVE -> List.of(
                    BloodType.AB_POSITIVE, BloodType.AB_NEGATIVE,
                    BloodType.A_POSITIVE, BloodType.A_NEGATIVE,
                    BloodType.B_POSITIVE, BloodType.B_NEGATIVE,
                    BloodType.O_POSITIVE, BloodType.O_NEGATIVE
                );
            };
        }

        // Do not apply RBC compatibility rules to other components
        return List.of(recipientType);
    }
}
