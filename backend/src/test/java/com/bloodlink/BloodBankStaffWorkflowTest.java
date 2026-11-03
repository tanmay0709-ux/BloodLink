package com.bloodlink;

import com.bloodlink.dto.*;
import com.bloodlink.model.*;
import com.bloodlink.repository.*;
import com.bloodlink.security.JwtTokenProvider;
import com.bloodlink.service.*;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BloodBankStaffWorkflowTest {

    @Mock private UserRepository userRepository;
    @Mock private HospitalRepository hospitalRepository;
    @Mock private BloodBankRepository bloodBankRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private BloodUnitRepository bloodUnitRepository;
    @Mock private DonationRepository donationRepository;
    @Mock private DonorRepository donorRepository;
    @Mock private BloodRequestRepository bloodRequestRepository;
    @Mock private BloodAllocationRepository bloodAllocationRepository;
    @Mock private BloodDispatchRepository bloodDispatchRepository;
    @Mock private BloodCompatibilityService bloodCompatibilityService;
    @Mock private NotificationService notificationService;

    private AuthService authService;
    private BloodBankService bloodBankService;
    private BloodUnitService bloodUnitService;
    private DonorService donorService;
    private BloodAllocationService bloodAllocationService;
    private AnalyticsService analyticsService;

    private BloodBank activeBloodBank1;
    private BloodBank activeBloodBank2;
    private BloodBank inactiveBloodBank;
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

        bloodBankService = new BloodBankService(bloodBankRepository, userRepository);

        bloodUnitService = new BloodUnitService(
            bloodUnitRepository,
            donationRepository,
            bloodBankRepository,
            userRepository
        );

        donorService = new DonorService(
            donorRepository,
            bloodBankRepository,
            userRepository
        );

        bloodAllocationService = new BloodAllocationService(
            bloodRequestRepository,
            bloodUnitRepository,
            bloodAllocationRepository,
            bloodDispatchRepository,
            bloodBankRepository,
            userRepository,
            bloodCompatibilityService,
            notificationService
        );

        analyticsService = new AnalyticsService(
            userRepository,
            bloodUnitRepository,
            bloodRequestRepository,
            donationRepository,
            donorRepository,
            bloodDispatchRepository,
            bloodAllocationRepository,
            bloodUnitService
        );

        activeBloodBank1 = BloodBank.builder()
            .id(1L)
            .name("City Blood Center")
            .address("100 Main St")
            .contactPhone("555-0101")
            .active(true)
            .build();

        activeBloodBank2 = BloodBank.builder()
            .id(2L)
            .name("Regional Red Cross")
            .address("200 Oak Ave")
            .contactPhone("555-0202")
            .active(true)
            .build();

        inactiveBloodBank = BloodBank.builder()
            .id(99L)
            .name("Closed Blood Bank")
            .active(false)
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
        lenient().when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    // =========================================================================
    // 1. REGISTRATION WORKFLOW & PENDING ASSOCIATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Blood bank staff registration with blood bank creates pending association without granted access")
    void testBloodBankStaffRegistration_Success_PendingStatus() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("bank_officer");
        req.setEmail("officer@cityblood.org");
        req.setPassword("Secret123!");
        req.setRole("BLOOD_BANK_STAFF");
        req.setBloodBankId(1L);

        when(userRepository.existsByUsername("bank_officer")).thenReturn(false);
        when(userRepository.existsByEmail("officer@cityblood.org")).thenReturn(false);
        when(bloodBankRepository.findById(1L)).thenReturn(Optional.of(activeBloodBank1));
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed_pw");

        UserDetails dummyDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("bank_officer")).thenReturn(dummyDetails);
        when(jwtTokenProvider.generateToken(dummyDetails)).thenReturn("jwt-token-123");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        JwtResponse res = authService.register(req);

        assertNotNull(res);
        assertEquals("bank_officer", res.getUsername());
        assertEquals("BLOOD_BANK_STAFF", res.getRole());
        assertEquals("PENDING", res.getApprovalStatus());
        assertEquals(1L, res.getPendingBloodBankId());
        assertEquals("City Blood Center", res.getPendingBloodBankName());
        assertNull(res.getAssignedBloodBankId(), "Access must NOT be granted upon registration");
        assertNull(res.getAssignedBloodBankName());

        verify(userRepository).save(argThat(u ->
            u.getRole() == User.Role.BLOOD_BANK_STAFF &&
            u.getApprovalStatus() == User.ApprovalStatus.PENDING &&
            u.getPendingBloodBank().getId().equals(1L) &&
            u.getAssignedBloodBank() == null
        ));
    }

    @Test
    @DisplayName("Blood bank staff registration without blood bank selection throws exception")
    void testBloodBankStaffRegistration_MissingBloodBank_ThrowsException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("bank_officer");
        req.setEmail("officer@cityblood.org");
        req.setPassword("Secret123!");
        req.setRole("BLOOD_BANK_STAFF");
        req.setBloodBankId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register(req)
        );
        assertTrue(ex.getMessage().contains("Blood bank selection is required"));
    }

    @Test
    @DisplayName("Blood bank staff registration with inactive blood bank throws exception")
    void testBloodBankStaffRegistration_InactiveBloodBank_ThrowsException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("bank_officer");
        req.setEmail("officer@cityblood.org");
        req.setPassword("Secret123!");
        req.setRole("BLOOD_BANK_STAFF");
        req.setBloodBankId(99L);

        when(bloodBankRepository.findById(99L)).thenReturn(Optional.of(inactiveBloodBank));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register(req)
        );
        assertTrue(ex.getMessage().contains("Selected blood bank is inactive"));
    }

    // =========================================================================
    // 2. ADMIN APPROVAL & REJECTION TESTS
    // =========================================================================

    @Test
    @DisplayName("Admin can view pending staff associations for a blood bank")
    void testAdmin_GetPendingStaff_BloodBank() {
        mockSecurityUser(adminUser);

        User pendingUser = User.builder()
            .id(20L)
            .username("pending_bank_user")
            .email("pending@cityblood.org")
            .role(User.Role.BLOOD_BANK_STAFF)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .pendingBloodBank(activeBloodBank1)
            .build();

        when(bloodBankRepository.findById(1L)).thenReturn(Optional.of(activeBloodBank1));
        when(userRepository.findAll()).thenReturn(List.of(pendingUser));

        List<UserSummaryDto> pendingList = bloodBankService.getPendingStaff(1L);

        assertEquals(1, pendingList.size());
        assertEquals("pending_bank_user", pendingList.get(0).getUsername());
        assertEquals("PENDING", pendingList.get(0).getApprovalStatus());
        assertEquals(1L, pendingList.get(0).getPendingBloodBankId());
    }

    @Test
    @DisplayName("Admin approval transitions staff to APPROVED, sets assignedBloodBank, and clears pendingBloodBank")
    void testAdmin_ApproveStaff_BloodBank_Success() {
        mockSecurityUser(adminUser);

        User pendingUser = User.builder()
            .id(25L)
            .username("nurse_bank")
            .email("nurse@cityblood.org")
            .role(User.Role.BLOOD_BANK_STAFF)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .pendingBloodBank(activeBloodBank1)
            .assignedBloodBank(null)
            .build();

        when(bloodBankRepository.findById(1L)).thenReturn(Optional.of(activeBloodBank1));
        when(userRepository.findById(25L)).thenReturn(Optional.of(pendingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserSummaryDto approvedDto = bloodBankService.approveStaff(1L, 25L);

        assertEquals("APPROVED", approvedDto.getApprovalStatus());
        assertEquals(1L, approvedDto.getAssignedBloodBankId());
        assertEquals("City Blood Center", approvedDto.getAssignedBloodBankName());
        assertNull(approvedDto.getPendingBloodBankId());

        assertEquals(User.ApprovalStatus.APPROVED, pendingUser.getApprovalStatus());
        assertEquals(activeBloodBank1, pendingUser.getAssignedBloodBank());
        assertNull(pendingUser.getPendingBloodBank());
    }

    @Test
    @DisplayName("Admin rejection transitions staff to REJECTED and clears associations")
    void testAdmin_RejectStaff_BloodBank_Success() {
        mockSecurityUser(adminUser);

        User pendingUser = User.builder()
            .id(26L)
            .username("rejected_bank_user")
            .email("reject@cityblood.org")
            .role(User.Role.BLOOD_BANK_STAFF)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .pendingBloodBank(activeBloodBank1)
            .assignedBloodBank(null)
            .build();

        when(bloodBankRepository.findById(1L)).thenReturn(Optional.of(activeBloodBank1));
        when(userRepository.findById(26L)).thenReturn(Optional.of(pendingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        bloodBankService.rejectStaff(1L, 26L);

        assertEquals(User.ApprovalStatus.REJECTED, pendingUser.getApprovalStatus());
        assertNull(pendingUser.getAssignedBloodBank());
        assertNull(pendingUser.getPendingBloodBank());
    }

    @Test
    @DisplayName("Security: Admin cannot approve themselves (prevent self-approval)")
    void testSecurity_PreventSelfApproval_BloodBank() {
        mockSecurityUser(adminUser);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
            bloodBankService.approveStaff(1L, adminUser.getId())
        );
        assertTrue(ex.getMessage().contains("cannot approve their own blood bank association"));
    }

    // =========================================================================
    // 3. AUTHORIZATION & FACILITY ISOLATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Security: Pending blood bank staff cannot manage blood units")
    void testSecurity_PendingStaff_CannotAccessUnits() {
        User pendingStaff = User.builder()
            .id(50L)
            .username("pending_staff")
            .role(User.Role.BLOOD_BANK_STAFF)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .pendingBloodBank(activeBloodBank1)
            .assignedBloodBank(null)
            .build();

        mockSecurityUser(pendingStaff);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
            bloodUnitService.getAll(null, null, null, null, null)
        );
        assertTrue(ex.getMessage().contains("not an approved member"));
    }

    @Test
    @DisplayName("Security: Pending blood bank staff cannot register donors")
    void testSecurity_PendingStaff_CannotRegisterDonor() {
        User pendingStaff = User.builder()
            .id(51L)
            .username("pending_donor_staff")
            .role(User.Role.BLOOD_BANK_STAFF)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .pendingBloodBank(activeBloodBank1)
            .assignedBloodBank(null)
            .build();

        mockSecurityUser(pendingStaff);

        DonorRequest req = new DonorRequest();
        req.setFullName("John Doe");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
            donorService.registerDonor(req)
        );
        assertTrue(ex.getMessage().contains("not an approved member"));
    }

    @Test
    @DisplayName("Security: Pending blood bank staff cannot allocate blood units")
    void testSecurity_PendingStaff_CannotAllocateUnits() {
        User pendingStaff = User.builder()
            .id(52L)
            .username("pending_alloc_staff")
            .role(User.Role.BLOOD_BANK_STAFF)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .pendingBloodBank(activeBloodBank1)
            .assignedBloodBank(null)
            .build();

        mockSecurityUser(pendingStaff);

        AllocationRequestDto req = new AllocationRequestDto();
        req.setUnitIds(List.of(101L));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
            bloodAllocationService.allocateUnits(1L, req)
        );
        assertTrue(ex.getMessage().contains("Staff member is not an approved member of any blood bank"));
    }

    @Test
    @DisplayName("Security: Facility Isolation prevents blood bank staff from accessing other bank's units")
    void testSecurity_FacilityIsolation_CannotAccessOtherBankUnits() {
        User staffBank1 = User.builder()
            .id(60L)
            .username("bank1_staff")
            .role(User.Role.BLOOD_BANK_STAFF)
            .approvalStatus(User.ApprovalStatus.APPROVED)
            .assignedBloodBank(activeBloodBank1)
            .build();

        mockSecurityUser(staffBank1);

        BloodUnit unitOfBank2 = BloodUnit.builder()
            .id(201L)
            .unitId("BU-201")
            .bloodBank(activeBloodBank2)
            .build();

        when(bloodUnitRepository.findById(201L)).thenReturn(Optional.of(unitOfBank2));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
            bloodUnitService.getById(201L)
        );
        assertTrue(ex.getMessage().contains("belonging to your assigned blood bank"));
    }

    // =========================================================================
    // 4. BACKWARD COMPATIBILITY & DASHBOARD ANALYTICS TESTS
    // =========================================================================

    @Test
    @DisplayName("Backward Compatibility: Existing staff with null approvalStatus but assigned blood bank is treated as approved")
    void testBackwardCompatibility_ExistingApprovedBloodBankStaff() {
        User legacyStaff = User.builder()
            .id(70L)
            .username("legacy_bank_staff")
            .role(User.Role.BLOOD_BANK_STAFF)
            .approvalStatus(null) // Legacy record before approvalStatus column was populated
            .assignedBloodBank(activeBloodBank1)
            .build();

        mockSecurityUser(legacyStaff);

        when(bloodUnitRepository.findAllByBloodBankId(1L))
            .thenReturn(Collections.emptyList());

        // Should NOT throw AccessDeniedException
        List<BloodUnitResponse> units = bloodUnitService.getAll(null, null, null, null, null);
        assertNotNull(units);
        assertTrue(units.isEmpty());
    }

    @Test
    @DisplayName("Graceful Dashboard Analytics: Unassigned or pending staff does not encounter 403 error")
    void testGracefulDashboardAnalytics_UnassignedOrPendingStaff() {
        User pendingStaff = User.builder()
            .id(80L)
            .username("pending_dash_staff")
            .role(User.Role.BLOOD_BANK_STAFF)
            .approvalStatus(User.ApprovalStatus.PENDING)
            .pendingBloodBank(activeBloodBank1)
            .assignedBloodBank(null)
            .build();

        mockSecurityUser(pendingStaff);

        DashboardAnalyticsDto analytics = analyticsService.getDashboardAnalytics();

        assertNotNull(analytics);
        assertEquals("Unassigned Blood Bank Staff", analytics.getFacilityName());
        assertEquals("BLOOD_BANK", analytics.getFacilityType());
        assertEquals(0L, analytics.getOverview().getTotalUnits());
        assertEquals(0L, analytics.getOverview().getAvailableUnits());
        assertEquals(0L, analytics.getOperationalAnalytics().getTotalDispatches());
        assertTrue(analytics.getOperationalAnalytics().getRecentDispatches().isEmpty());
        assertTrue(analytics.getRequestAnalytics().getRecentRequests().isEmpty());
    }
}
