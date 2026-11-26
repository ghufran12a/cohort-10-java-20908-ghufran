package com.contactapp.contactmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contactapp.contactmanager.dto.ContactRequest;
import com.contactapp.contactmanager.dto.EmailDto;
import com.contactapp.contactmanager.dto.ImportResult;
import com.contactapp.contactmanager.dto.PhoneDto;
import com.contactapp.contactmanager.entity.Contact;
import com.contactapp.contactmanager.entity.Email;
import com.contactapp.contactmanager.entity.Phone;
import com.contactapp.contactmanager.exception.BadRequestException;
import com.contactapp.contactmanager.repository.ContactRepository;
import jakarta.validation.Validation;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactCsvServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private ContactService contactService;

    private ContactCsvService csvService;

    @BeforeEach
    void setUp() {
        // a REAL validator, so the import is checked against the real rules
        csvService = new ContactCsvService(contactRepository, contactService,
                Validation.buildDefaultValidatorFactory().getValidator());
    }

    private static InputStream csv(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private String exportOf(Contact contact) {
        when(contactRepository.findByUserIdAndDeletedAtIsNullOrderByFirstNameAscLastNameAsc(1L))
                .thenReturn(List.of(contact));
        return new String(csvService.exportCsv(1L), StandardCharsets.UTF_8);
    }

    private static Contact contact(String firstName) {
        Contact c = new Contact();
        c.setFirstName(firstName);
        c.setLastName("Khan, Junior");
        c.addPhone(new Phone("03001234567", "work"));
        c.addPhone(new Phone("+923331112222", "home"));
        c.addEmail(new Email("ali@test.com", "work"));
        return c;
    }

    @Test
    void exportWritesHeaderAllValuesAndQuotesCommas() {
        String file = exportOf(contact("Ali"));

        assertThat(file).contains("First Name,Last Name,Title,Phones,Emails")
                .contains("\"Khan, Junior\"")
                .contains("work:03001234567; home:+923331112222")
                .contains("work:ali@test.com");
    }

    @Test
    void exportNeutralisesExcelFormulas() {
        assertThat(exportOf(contact("=HYPERLINK(1)"))).contains("'=HYPERLINK(1)");
    }

    @Test
    void anExportedFileCanBeImportedAgainUnchanged() {
        String file = exportOf(contact("=Ali"));

        ImportResult result = csvService.importCsv(1L, csv(file));

        ArgumentCaptor<ContactRequest> saved = ArgumentCaptor.forClass(ContactRequest.class);
        verify(contactService).create(eq(1L), saved.capture());
        assertThat(result.imported()).isEqualTo(1);
        assertThat(saved.getValue().firstName()).isEqualTo("=Ali");
        assertThat(saved.getValue().lastName()).isEqualTo("Khan, Junior");
        assertThat(saved.getValue().phones()).extracting(PhoneDto::phone)
                .containsExactly("03001234567", "+923331112222");
        assertThat(saved.getValue().emails()).extracting(EmailDto::label).containsExactly("work");
    }

    @Test
    void goodRowsAreImportedAndBadRowsAreExplained() {
        String file = """
                first name,LAST NAME,Phones
                Ali,Khan,03001234567
                Sara,Ahmed,abc
                ,Nobody,03001234567
                Omar,Zed,
                """;

        ImportResult result = csvService.importCsv(1L, csv(file));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(3);
        assertThat(result.errors()).hasSize(3);
        assertThat(result.errors().get(0)).startsWith("Row 3:").contains("digits");
        assertThat(result.errors().get(1)).isEqualTo("Row 4: First name is required");
        assertThat(result.errors().get(2)).isEqualTo("Row 5: At least one phone number is required");
        verify(contactService, times(1)).create(eq(1L), any());
    }

    @Test
    void aPhoneWithoutALabelGetsTheLabelOther() {
        csvService.importCsv(1L, csv("First Name,Last Name,Phones\nAli,Khan,03001234567\n"));

        ArgumentCaptor<ContactRequest> saved = ArgumentCaptor.forClass(ContactRequest.class);
        verify(contactService).create(eq(1L), saved.capture());
        assertThat(saved.getValue().phones()).extracting(PhoneDto::label).containsExactly("other");
    }

    @Test
    void missingColumnsAreRejected() {
        InputStream file = csv("Name,Number\nAli,0300\n");

        assertThatThrownBy(() -> csvService.importCsv(1L, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("columns");
        verify(contactService, never()).create(any(), any());
    }

    @Test
    void aBrokenFileIsRejected() {
        InputStream file = csv("First Name,Last Name,Phones\n\"Ali,Khan,0300");

        assertThatThrownBy(() -> csvService.importCsv(1L, file)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void aFileWithOnlyAHeaderIsRejected() {
        InputStream file = csv("First Name,Last Name,Phones\n");

        assertThatThrownBy(() -> csvService.importCsv(1L, file)).hasMessage("The file has no contacts");
    }

    @Test
    void tooManyRowsAreRejectedBeforeAnythingIsSaved() {
        String file = "First Name,Last Name,Phones\n" + "A,B,03001234567\n".repeat(ContactCsvService.MAX_ROWS + 1);
        InputStream stream = csv(file);

        assertThatThrownBy(() -> csvService.importCsv(1L, stream)).hasMessageContaining("at most");
        verify(contactService, never()).create(any(), any());
    }
}
