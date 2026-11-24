package com.contactapp.contactmanager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contactapp.contactmanager.config.JwtConfig;
import com.contactapp.contactmanager.config.SecurityConfig;
import com.contactapp.contactmanager.dto.AuthResponse;
import com.contactapp.contactmanager.dto.UserResponse;
import com.contactapp.contactmanager.exception.DuplicateResourceException;
import com.contactapp.contactmanager.exception.InvalidCredentialsException;
import com.contactapp.contactmanager.service.AuthService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AuthController.class, UserController.class})
@Import({SecurityConfig.class, JwtConfig.class})
class AuthAndUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private static UserResponse user() {
        return new UserResponse(7L, "Ali", "ali@test.com", null, LocalDateTime.now());
    }

    @Test
    void registerIsPublicAndReturns201() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse("tok", "Bearer", 60, user()));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ali\",\"email\":\"ali@test.com\",\"password\":\"Secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("tok"));
    }

    @Test
    void registerWithWeakPasswordReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ali\",\"email\":\"ali@test.com\",\"password\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
        verifyNoInteractions(authService);
    }

    @Test
    void duplicateRegistrationReturns409() throws Exception {
        when(authService.register(any())).thenThrow(new DuplicateResourceException("This email is already registered"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ali\",\"email\":\"ali@test.com\",\"password\":\"Secret123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongLoginReturns401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"ali@test.com\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email/phone or password"));
    }

    @Test
    void profileNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void profileReturnsTheLoggedInUserWithoutAnyPassword() throws Exception {
        when(authService.getProfile(7L)).thenReturn(user());

        mockMvc.perform(get("/api/users/me").with(jwt().jwt(t -> t.subject("7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ali"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void changePasswordReturns204() throws Exception {
        mockMvc.perform(put("/api/users/me/password").with(jwt().jwt(t -> t.subject("7")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Secret123\",\"newPassword\":\"NewSecret456\"}"))
                .andExpect(status().isNoContent());
        verify(authService).changePassword(eq(7L), any());
    }
}
