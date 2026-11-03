package com.bloodlink;

import com.bloodlink.dto.BloodRequestCreateDto;
import com.bloodlink.dto.BloodRequestResponse;
import com.bloodlink.dto.JwtResponse;
import com.bloodlink.dto.RegisterRequest;
import com.bloodlink.dto.UserSummaryDto;
import com.bloodlink.model.BloodBank;
import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.BloodRequest;
import com.bloodlink.model.BloodUnit;
import com.bloodlink.model.BloodUnit.BloodComponentType;
import com.bloodlink.model.EmergencyRequest;
import com.bloodlink.model.Hospital;
import com.bloodlink.model.User;
import com.bloodlink.repository.BloodAllocationRepository;
import com.bloodlink.repository.BloodBankRepository;
import com.bloodlink.repository.BloodRequestRepository;
import com.bloodlink.repository.BloodUnitRepository;
import com.bloodlink.repository.EmergencyRequestRepository;
import com.bloodlink.repository.HospitalRepository;
import com.bloodlink.repository.UserRepository;
import com.bloodlink.security.JwtTokenProvider;
import com.bloodlink.service.AuthService;
import com.bloodlink.service.BloodCompatibilityService;
import com.bloodlink.service.BloodRequestService;
import com.bloodlink.service.BloodUnitService;
import com.bloodlink.service.EmergencyRequestService;
import com.bloodlink.service.HospitalService;
import com.bloodlink.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HospitalStaffWorkflowTest {

    @Mock private UserRepository userRepository;
    @Mock private HospitalRepository hospitalRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private BloodRequestRepository bloodRequestRepository;
    @Mock private BloodBankRepository bloodBankRepository;
    @Mock private BloodUnitRepository bloodUnitRepository;
    @Mock private BloodCompatibilityService bloodCompatibilityService;
    @Mock private BloodUnitService bloodUnitService;
    @Mock private BloodAllocationRepository bloodAllocationRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmergencyRequestRepository emergencyRequestRepository;

    private AuthService authService;
    private HospitalService hospitalService;
    private BloodRequestService bloodRequestService;
    private EmergencyRequestService emergencyRequestService;

    private Hospital activeHospital;
    private Hospital inactiveHospital;
    private BloodBank activeBloodBank;
    private User adminUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            hospitalRepository,
            bloodBankRepository,
            passwordEncoder,
            jwtTokenProvider,
            authenticationManager,
            userDetailsService
        );

        hospitalService = new HospitalService(hospitalRepository, userRepository);

        bloodRequestService = new BloodRequestService(
            bloodRequestRepository,
            hospitalRepository,
            bloodBankRepository,
            bloodUnitRepository,
            userRepository,
            bloodCompatibilityService,
            bloodUnitService,
            bloodAllocationRepository,
            notificationService
        );

        emergencyRequestService = new EmergencyRequestService(
            emergencyRequestRepository,
            userRepository
        );

        activeHospital = Hospital.builder()
            .id(1L)
            .name("Metro General Hospital")
            .address("123 Health Ave")
            .contactPhone("555-0100")
            .active(true)
            .build();

        inactiveHospital = Hospital.builder()
            .id(2L)
            .name("Closed Regional Clinic")
            .active(false)
            .build();

        activeBloodBank = BloodBank.builder()
            .id(1L)
            .name("City Blood Center")
            .active(true)
            .build();

        adminUser = User.builder()
            .id(999L)
            .username("admin")
            .email("admin@bloodlink.org")
            .role(User.Role.SYSTEM_ADMIN)
            .approvalStatus(User.ApprovalStatus.APPROVED)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityUser(User user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            "pass",
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    // =========================================================================
    // 1. REGISTRATION WORKFLOW & PENDING ASSOCIATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Hospital staff registration with hospital creates pending association without granted access")
    void testHospitalStaffRegistration_Success_PendingStatus() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("dr_smith");
        req.setEmail("smith@metro.org");
        req.setPassword("Secret123!");
        req.setRole("HOSPITAL_STAFF");
        req.setHospitalId(1L);

        when(userRepository.existsByUsername("dr_smith")).thenReturn(false);
        when(userRepository.existsByEmail("smith@metro.org")).thenReturn(false);
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(activeHospital));
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed_pw");

        UserDetails dummyDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("dr_smith")).thenReturn(dummyDetails);
        when(jwtTokenProvider.generateToken(dummyDetails)).thenReturn("jwt-token-123");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        JwtResponse res = authService.register(req);

        assertNotNull(res);
        assertEquals("dr_smith", res.getUsername());
        assertEquals("HOSPITAL_STAFF", res.getRole());
        assertEquals("PENDING", res.getApprovalStatus());
        assertEquals(1L, res.getPendingHospitalId());
        assertEquals("Metro General Hospital", res.getPendingHospitalName());
        assertNull(res.getAssignedHospitalId(), "Access must NOT be granted yet upon registration");
        assertNull(res.getAssignedHospitalName());

        verify(userRepository).save(argThat(u ->
            u.getRole() == User.Role.HOSPITAL_STAFF &&
            u.getApprovalStatus() == User.ApprovalStatus.PENDING &&
            u.getPendingHospital().getId().equals(1L) &&
            u.getAssignedHospital() == null
        ));
    }

    @Test
    @DisplayName("Hospital staff registration without hospital selection throws exception")
    void testHospitalStaffRegistration_MissingHospital_ThrowsException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("dr_smith");
        req.setEmail("smith@metro.org");
        req.setPassword("Secret123!");
        req.setRole("HOSPITAL_STAFF");
        req.setHospitalId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register(req)
        );
        assertTrue(ex.getMessage().contains("Hospital selection is required"));
    }

    @Test
    @DisplayName("Hospital staff registration with inactive hospital throws exception")
    void testHospitalStaffRegistration_InactiveHospital_ThrowsException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("dr_smith");
        req.setEmail("smith@metro.org");
        req.setPassword("Secret123!");
        req.setRole("HOSPITAL_STAFF");
        req.setHospitalId(2L);

        when(hospitalRepository.findById(2L)).thenReturn(Optional.of(inactiveHospital));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register(req)
        );
        assertTrue(ex.getMessage().contains("Selected hospital is inactive"));
    }

    @Test
    @DisplayName("Blood bank staff registration with blood bank creates pending association without granted access")
    void testBloodBankStaffRegistration_ApprovedStatus() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("bank_officer");
        req.setEmail("officer@redcross.org");
        req.setPassword("Secret123!");
        req.setRole("BLOOD_BANK_STAFF");
        req.setBloodBankId(1L);

        when(userRepository.existsByUsername("bank_officer")).thenReturn(false);
        when(userRepository.existsByEmail("officer@redcross.org")).thenReturn(false);
        when(bloodBankRepository.findById(1L)).thenReturn(Optional.of(activeBloodBank));
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed_pw");

        UserDetails dummyDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("bank_officer")).thenReturn(dummyDetails);
        when(jwtTokenProvider.generateToken(dummyDetails)).thenReturn("jwt-token-456");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(20L);
            return u;
        });

        JwtResponse res = authService.register(req);

        assertEquals("BLOOD_BANK_STAFF", res.getRole());
        assertEquals("PENDING", res.getApprovalStatus());
        assertEquals(1L, res.getPendingBloodBankId());
        assertEquals("City Blood Center", res.getPendingBloodBankName());
        assertNull(res.getAssignedBloodBankId());
        assertNull(res.getPendingHospitalId());
    }

    // =========================================================================
    // 2. ADMIN APPROVAL & REJECTION WORKFLOW TESTS
    // =========================================================================

    @Test
    @DisplayName("Admin can view pending staff requests for a hospital")
    void testAdmin_GetPendingStaff() {
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(activeHospital));

        User pendingUser = User.builder()
            .id(10L)
            .username("dr_smith")
            .email("smith@metro.org")
            .role(User.Role.HOSPITAL_STAFF)
            .pendingHospital(activeHospital)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .build();

        User approvedUser = User.builder()
            .id(11L)
            .username("dr_jones")
            .email("jones@metro.org")
            .role(User.Role.HOSPITAL_STAFF)
            .assignedHospital(activeHospital)
            .approvalStatus(User.ApprovalStatus.APPROVED)
            .build();

        when(userRepository.findAll()).thenReturn(List.of(pendingUser, approvedUser));

        List<UserSummaryDto> pendingList = hospitalService.getPendingStaff(1L);

        assertEquals(1, pendingList.size());
        assertEquals("dr_smith", pendingList.get(0).getUsername());
        assertEquals("PENDING", pendingList.get(0).getApprovalStatus());
        assertEquals(1L, pendingList.get(0).getPendingHospitalId());
    }

    @Test
    @DisplayName("Admin approving staff grants access and assigns hospital")
    void testAdmin_ApproveStaff_Success() {
        mockSecurityUser(adminUser);

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(activeHospital));

        User pendingUser = User.builder()
            .id(10L)
            .username("dr_smith")
            .email("smith@metro.org")
            .role(User.Role.HOSPITAL_STAFF)
            .pendingHospital(activeHospital)
            .assignedHospital(null)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(pendingUser));

        UserSummaryDto approvedDto = hospitalService.approveStaff(1L, 10L);

        assertEquals(1L, approvedDto.getAssignedHospitalId());
        assertEquals("Metro General Hospital", approvedDto.getAssignedHospitalName());
        assertEquals("APPROVED", approvedDto.getApprovalStatus());
        assertNull(approvedDto.getPendingHospitalId());

        assertEquals(activeHospital, pendingUser.getAssignedHospital());
        assertNull(pendingUser.getPendingHospital());
        assertEquals(User.ApprovalStatus.APPROVED, pendingUser.getApprovalStatus());
        verify(userRepository).save(pendingUser);
    }

    @Test
    @DisplayName("Admin rejecting staff clears association and sets status to REJECTED")
    void testAdmin_RejectStaff_Success() {
        mockSecurityUser(adminUser);

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(activeHospital));

        User pendingUser = User.builder()
            .id(10L)
            .username("dr_smith")
            .email("smith@metro.org")
            .role(User.Role.HOSPITAL_STAFF)
            .pendingHospital(activeHospital)
            .assignedHospital(null)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(pendingUser));

        hospitalService.rejectStaff(1L, 10L);

        assertNull(pendingUser.getAssignedHospital());
        assertNull(pendingUser.getPendingHospital());
        assertEquals(User.ApprovalStatus.REJECTED, pendingUser.getApprovalStatus());
        verify(userRepository).save(pendingUser);
    }

    @Test
    @DisplayName("Security: Users cannot approve their own affiliation")
    void testSecurity_PreventSelfApproval() {
        User selfAdminStaff = User.builder()
            .id(50L)
            .username("admin_staff")
            .email("admin_staff@metro.org")
            .role(User.Role.SYSTEM_ADMIN)
            .build();

        mockSecurityUser(selfAdminStaff);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
            hospitalService.approveStaff(1L, 50L)
        );
        assertTrue(ex.getMessage().contains("Users cannot approve their own"));
    }

    // =========================================================================
    // 3. REQUEST CREATION & SECURITY RESTRICTION TESTS
    // =========================================================================

    @Test
    @DisplayName("Security: Pending staff cannot create blood requests")
    void testSecurity_PendingStaff_CannotCreateBloodRequest() {
        User pendingUser = User.builder()
            .id(10L)
            .username("dr_smith")
            .email("smith@metro.org")
            .role(User.Role.HOSPITAL_STAFF)
            .pendingHospital(activeHospital)
            .assignedHospital(null)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .build();

        mockSecurityUser(pendingUser);

        BloodRequestCreateDto dto = new BloodRequestCreateDto();
        dto.setBloodType(BloodInventory.BloodType.O_POSITIVE);
        dto.setComponentType(BloodUnit.BloodComponentType.RED_BLOOD_CELLS);
        dto.setQuantity(2);
        dto.setRequiredDate(LocalDate.now().plusDays(2));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
            bloodRequestService.createRequest(dto)
        );
        assertTrue(ex.getMessage().contains("not an approved member of any hospital"));
    }

    @Test
    @DisplayName("Security: Approved staff can create blood requests and hospital is automatically assigned")
    void testSecurity_ApprovedStaff_CanCreateBloodRequest_AutoHospital() {
        User approvedUser = User.builder()
            .id(10L)
            .username("dr_smith")
            .email("smith@metro.org")
            .role(User.Role.HOSPITAL_STAFF)
            .assignedHospital(activeHospital)
            .approvalStatus(User.ApprovalStatus.APPROVED)
            .build();

        mockSecurityUser(approvedUser);

        BloodRequestCreateDto dto = new BloodRequestCreateDto();
        dto.setBloodType(BloodInventory.BloodType.O_POSITIVE);
        dto.setComponentType(BloodUnit.BloodComponentType.RED_BLOOD_CELLS);
        dto.setQuantity(2);
        dto.setRequiredDate(LocalDate.now().plusDays(2));

        when(bloodRequestRepository.save(any(BloodRequest.class))).thenAnswer(invocation -> {
            BloodRequest r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        BloodRequestResponse res = bloodRequestService.createRequest(dto);

        assertNotNull(res);
        assertEquals(100L, res.getId());
        assertEquals(1L, res.getHospitalId());
        assertEquals("Metro General Hospital", res.getHospitalName());

        verify(bloodRequestRepository).save(argThat(r ->
            r.getHospital().equals(activeHospital) &&
            r.getCreatedBy().equals(approvedUser)
        ));
    }

    @Test
    @DisplayName("Security: Pending staff cannot create legacy emergency requests")
    void testSecurity_PendingStaff_CannotCreateEmergencyRequest() {
        User pendingUser = User.builder()
            .id(10L)
            .username("dr_smith")
            .role(User.Role.HOSPITAL_STAFF)
            .pendingHospital(activeHospital)
            .assignedHospital(null)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .build();

        mockSecurityUser(pendingUser);

        EmergencyRequest req = EmergencyRequest.builder()
            .bloodType(BloodInventory.BloodType.A_POSITIVE)
            .quantity(3)
            .hospital("Arbitrary Hospital")
            .build();

        assertThrows(AccessDeniedException.class, () ->
            emergencyRequestService.createRequest(req)
        );
    }

    @Test
    @DisplayName("Security: Approved staff creating emergency request automatically enforces assigned hospital")
    void testSecurity_ApprovedStaff_EmergencyRequest_EnforcesAssignedHospital() {
        User approvedUser = User.builder()
            .id(10L)
            .username("dr_smith")
            .role(User.Role.HOSPITAL_STAFF)
            .assignedHospital(activeHospital)
            .approvalStatus(User.ApprovalStatus.APPROVED)
            .build();

        mockSecurityUser(approvedUser);

        EmergencyRequest req = EmergencyRequest.builder()
            .bloodType(BloodInventory.BloodType.A_POSITIVE)
            .quantity(3)
            .hospital("Spoofed Hospital Name")
            .build();

        when(emergencyRequestRepository.save(any(EmergencyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyRequest saved = emergencyRequestService.createRequest(req);

        assertEquals("Metro General Hospital", saved.getHospital(), "Hospital name must automatically be overridden with user's assigned hospital");
    }

    @Test
    @DisplayName("Existing approved accounts without explicit approval_status column are treated as approved")
    void testBackwardCompatibility_ExistingApprovedAccounts() {
        // User created before approval_status column was introduced (approvalStatus is null, but assignedHospital is set)
        User existingUser = User.builder()
            .id(99L)
            .username("dr_legacy")
            .email("legacy@metro.org")
            .role(User.Role.HOSPITAL_STAFF)
            .assignedHospital(activeHospital)
            .approvalStatus(null)
            .build();

        mockSecurityUser(existingUser);

        BloodRequestCreateDto dto = new BloodRequestCreateDto();
        dto.setBloodType(BloodInventory.BloodType.B_POSITIVE);
        dto.setComponentType(BloodUnit.BloodComponentType.RED_BLOOD_CELLS);
        dto.setQuantity(1);
        dto.setRequiredDate(LocalDate.now().plusDays(1));

        when(bloodRequestRepository.save(any(BloodRequest.class))).thenAnswer(invocation -> {
            BloodRequest r = invocation.getArgument(0);
            r.setId(101L);
            return r;
        });

        BloodRequestResponse res = bloodRequestService.createRequest(dto);

        assertNotNull(res);
        assertEquals("Metro General Hospital", res.getHospitalName());
    }
}
