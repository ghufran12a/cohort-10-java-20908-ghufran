package com.contactapp.contactmanager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contactapp.contactmanager.config.JwtConfig;
import com.contactapp.contactmanager.config.SecurityConfig;
import com.contactapp.contactmanager.dto.ContactResponse;
import com.contactapp.contactmanager.exception.ResourceNotFoundException;
import com.contactapp.contactmanager.service.ContactService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * CONTROLLER test. @WebMvcTest starts only the web layer (controllers, security, exception handler).
 * The service is a mock. MockMvc sends fake HTTP requests without starting a real server.
 */
@WebMvcTest(ContactController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class ContactControllerTest {

    private static final String VALID_JSON = """
            {"firstName":"Ali","lastName":"Khan","phones":[{"phone":"03001234567","label":"home"}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactService contactService;

    // pretend we are logged in as user 7
    private static RequestPostProcessor loggedIn() {
        return jwt().jwt(token -> token.subject("7"));
    }

    private static ContactResponse response() {
        return new ContactResponse(1L, "Ali", "Khan", null, LocalDateTime.now(), null, List.of(), List.of());
    }

    @Test
    void requestsWithoutATokenAreRejected() throws Exception {
        mockMvc.perform(get("/api/contacts")).andExpect(status().isUnauthorized());
        verifyNoInteractions(contactService);
    }

    @Test
    void listPassesTheTokensUserIdAndLimitsThePageSize() throws Exception {
        PageRequest expected = PageRequest.of(0, 50, Sort.by("firstName", "lastName"));
        when(contactService.list(7L, "ali", expected)).thenReturn(new PageImpl<>(List.of(response()), expected, 1));

        mockMvc.perform(get("/api/contacts?search=ali&page=-3&size=999").with(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("Ali"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createReturns201() throws Exception {
        when(contactService.create(eq(7L), any())).thenReturn(response());

        mockMvc.perform(post("/api/contacts").with(loggedIn())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createWithBadPhoneReturns400WithFieldMessage() throws Exception {
        String badJson = """
                {"firstName":"Ali","lastName":"Khan","phones":[{"phone":"abc","label":"home"}]}
                """;

        mockMvc.perform(post("/api/contacts").with(loggedIn())
                        .contentType(MediaType.APPLICATION_JSON).content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors['phones[0].phone']").exists());
        verifyNoInteractions(contactService);
    }

    @Test
    void brokenJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/contacts").with(loggedIn())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingContactReturns404() throws Exception {
        when(contactService.get(7L, 99L)).thenThrow(new ResourceNotFoundException("Contact not found"));

        mockMvc.perform(get("/api/contacts/99").with(loggedIn()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Contact not found"));
    }

    @Test
    void textInsteadOfIdReturns400() throws Exception {
        mockMvc.perform(get("/api/contacts/abc").with(loggedIn())).andExpect(status().isBadRequest());
    }

    @Test
    void unexpectedErrorsReturn500WithoutInternalDetails() throws Exception {
        when(contactService.get(7L, 1L)).thenThrow(new IllegalStateException("secret internal detail"));

        mockMvc.perform(get("/api/contacts/1").with(loggedIn()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong. Please try again later."));
    }

    @Test
    void updateReturns200() throws Exception {
        when(contactService.update(eq(7L), eq(1L), any())).thenReturn(response());

        mockMvc.perform(put("/api/contacts/1").with(loggedIn())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/contacts/1").with(loggedIn())).andExpect(status().isNoContent());
        verify(contactService).delete(7L, 1L);
    }

    @Test
    void deletingSomeoneElsesContactReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Contact not found")).when(contactService).delete(7L, 2L);

        mockMvc.perform(delete("/api/contacts/2").with(loggedIn())).andExpect(status().isNotFound());
    }

    @Test
    void trashAndRestoreWork() throws Exception {
        when(contactService.listTrash(7L)).thenReturn(List.of(response()));
        when(contactService.restore(7L, 1L)).thenReturn(response());

        mockMvc.perform(get("/api/contacts/trash").with(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Ali"));
        mockMvc.perform(post("/api/contacts/1/restore").with(loggedIn())).andExpect(status().isOk());
    }
}
