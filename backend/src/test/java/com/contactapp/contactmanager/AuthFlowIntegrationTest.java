package com.contactapp.contactmanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.contactapp.contactmanager.dto.AuthResponse;
import com.contactapp.contactmanager.dto.ChangePasswordRequest;
import com.contactapp.contactmanager.dto.LoginRequest;
import com.contactapp.contactmanager.dto.RegisterRequest;
import com.contactapp.contactmanager.exception.InvalidCredentialsException;
import com.contactapp.contactmanager.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** INTEGRATION test: the whole application (real service, repository, H2 database, real JWT). */
@SpringBootTest
@Transactional // undo all database changes after the test
class AuthFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void registerThenChangePasswordThenLogin() {
        AuthResponse registered = authService.register(
                new RegisterRequest("Ali", "flow@test.com", null, "Secret123"));

        // the token we hand out is valid and says who the user is
        Jwt jwt = jwtDecoder.decode(registered.token());
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(registered.user().id()));

        authService.changePassword(registered.user().id(),
                new ChangePasswordRequest("Secret123", "NewSecret456"));

        LoginRequest oldLogin = new LoginRequest("flow@test.com", "Secret123");
        assertThatThrownBy(() -> authService.login(oldLogin)).isInstanceOf(InvalidCredentialsException.class);
        assertThat(authService.login(new LoginRequest("flow@test.com", "NewSecret456")).token()).isNotBlank();
    }
}
