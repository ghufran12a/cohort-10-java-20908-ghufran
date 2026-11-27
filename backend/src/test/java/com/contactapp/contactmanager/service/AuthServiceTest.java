package com.contactapp.contactmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contactapp.contactmanager.dto.AuthResponse;
import com.contactapp.contactmanager.dto.ChangePasswordRequest;
import com.contactapp.contactmanager.dto.LoginRequest;
import com.contactapp.contactmanager.dto.RegisterRequest;
import com.contactapp.contactmanager.entity.User;
import com.contactapp.contactmanager.exception.BadRequestException;
import com.contactapp.contactmanager.exception.DuplicateResourceException;
import com.contactapp.contactmanager.exception.InvalidCredentialsException;
import com.contactapp.contactmanager.exception.ResourceNotFoundException;
import com.contactapp.contactmanager.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    // a REAL BCrypt encoder, so we truly test that passwords are hashed
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthService authService;

    @BeforeEach // runs before every test, so each test starts fresh
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    private User userWithPassword(String rawPassword) {
        User user = new User();
        user.setId(1L);
        user.setName("Ali");
        user.setEmail("ali@test.com");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }

    // 38 characters, but 74 bytes in UTF-8 (each accented e needs 2 bytes)
    private static final String TOO_MANY_BYTES = "A1" + "\u00e9".repeat(36);

    @Test
    void registerRejectsAPasswordLongerThan72Bytes() {
        RegisterRequest request = new RegisterRequest("Ali", "ali@test.com", null, TOO_MANY_BYTES);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too long");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordRejectsAPasswordLongerThan72Bytes() {
        User user = userWithPassword("Secret123");
        String oldHash = user.getPasswordHash();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ChangePasswordRequest request = new ChangePasswordRequest("Secret123", TOO_MANY_BYTES);

        assertThatThrownBy(() -> authService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too long");
        assertThat(user.getPasswordHash()).isEqualTo(oldHash);
    }

    @Test
    void registerStoresAHashNotThePassword() {
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("token-123");

        AuthResponse response = authService.register(
                new RegisterRequest("Ali", "Ali@Test.com", null, "Secret123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture()); // grab what was saved
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("Secret123");
        assertThat(passwordEncoder.matches("Secret123", saved.getValue().getPasswordHash())).isTrue();
        assertThat(saved.getValue().getEmail()).isEqualTo("ali@test.com");
        assertThat(response.token()).isEqualTo("token-123");
    }

    @Test
    void registerWorksWithPhoneOnly() {
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        AuthResponse response = authService.register(
                new RegisterRequest("Sara", "", "03211234567", "Passw0rd9"));

        assertThat(response.user().email()).isNull();
        assertThat(response.user().phone()).isEqualTo("03211234567");
    }

    @Test
    void registerNeedsEmailOrPhone() {
        RegisterRequest request = new RegisterRequest("X", " ", null, "Secret123");

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("ali@test.com")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("Ali", "ali@test.com", null, "Secret123");

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void registerRejectsDuplicatePhone() {
        when(userRepository.existsByPhone("03001234567")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("Ali", null, "03001234567", "Secret123");

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void loginByEmailWorks() {
        when(userRepository.findByEmail("ali@test.com")).thenReturn(Optional.of(userWithPassword("Secret123")));
        when(jwtService.generateToken(any())).thenReturn("token-123");

        AuthResponse response = authService.login(new LoginRequest("ALI@test.com", "Secret123"));

        assertThat(response.token()).isEqualTo("token-123");
    }

    @Test
    void loginByPhoneWorks() {
        when(userRepository.findByPhone("03001234567")).thenReturn(Optional.of(userWithPassword("Secret123")));

        AuthResponse response = authService.login(new LoginRequest("03001234567", "Secret123"));

        assertThat(response.user().id()).isEqualTo(1L);
    }

    @Test
    void loginFailsWithWrongPassword() {
        when(userRepository.findByEmail("ali@test.com")).thenReturn(Optional.of(userWithPassword("Secret123")));
        LoginRequest request = new LoginRequest("ali@test.com", "WrongPass1");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginFailsForUnknownUserWithTheSameError() {
        when(userRepository.findByEmail("no@test.com")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest("no@test.com", "Secret123");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void getProfileReturnsTheUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithPassword("Secret123")));

        assertThat(authService.getProfile(1L).name()).isEqualTo("Ali");
    }

    @Test
    void getProfileFailsWhenUserIsMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getProfile(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePasswordStoresTheNewHash() {
        User user = userWithPassword("Secret123");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.changePassword(1L, new ChangePasswordRequest("Secret123", "NewSecret456"));

        assertThat(passwordEncoder.matches("NewSecret456", user.getPasswordHash())).isTrue();
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithPassword("Secret123")));
        ChangePasswordRequest request = new ChangePasswordRequest("Wrong1234", "NewSecret456");

        assertThatThrownBy(() -> authService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    void changePasswordRejectsSamePassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithPassword("Secret123")));
        ChangePasswordRequest request = new ChangePasswordRequest("Secret123", "Secret123");

        assertThatThrownBy(() -> authService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class);
    }
}
