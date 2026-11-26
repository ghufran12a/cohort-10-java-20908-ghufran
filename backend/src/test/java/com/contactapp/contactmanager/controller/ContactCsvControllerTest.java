package com.contactapp.contactmanager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contactapp.contactmanager.config.JwtConfig;
import com.contactapp.contactmanager.config.SecurityConfig;
import com.contactapp.contactmanager.dto.ImportResult;
import com.contactapp.contactmanager.service.ContactCsvService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContactCsvController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class ContactCsvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactCsvService csvService;

    @Test
    void exportDownloadsAFileForTheLoggedInUser() throws Exception {
        when(csvService.exportCsv(7L)).thenReturn("First Name\n".getBytes());

        mockMvc.perform(get("/api/contacts/export").with(jwt().jwt(t -> t.subject("7"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"contacts.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string("First Name\n"));
    }

    @Test
    void importReturnsTheReport() throws Exception {
        when(csvService.importCsv(eq(7L), any())).thenReturn(new ImportResult(2, 1, List.of("Row 3: bad")));
        MockMultipartFile file = new MockMultipartFile("file", "c.csv", "text/csv", "data".getBytes());

        mockMvc.perform(multipart("/api/contacts/import").file(file).with(jwt().jwt(t -> t.subject("7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.errors[0]").value("Row 3: bad"));
    }

    @Test
    void anEmptyFileIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "c.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/contacts/import").file(file).with(jwt().jwt(t -> t.subject("7"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The file is empty"));
        verifyNoInteractions(csvService);
    }

    @Test
    void exportNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/contacts/export")).andExpect(status().isUnauthorized());
        verifyNoInteractions(csvService);
    }
}
