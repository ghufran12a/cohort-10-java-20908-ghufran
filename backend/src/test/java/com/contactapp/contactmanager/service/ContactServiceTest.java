package com.contactapp.contactmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contactapp.contactmanager.dto.ContactRequest;
import com.contactapp.contactmanager.dto.ContactResponse;
import com.contactapp.contactmanager.dto.EmailDto;
import com.contactapp.contactmanager.dto.PhoneDto;
import com.contactapp.contactmanager.entity.Contact;
import com.contactapp.contactmanager.entity.Email;
import com.contactapp.contactmanager.entity.Phone;
import com.contactapp.contactmanager.entity.User;
import com.contactapp.contactmanager.exception.ResourceNotFoundException;
import com.contactapp.contactmanager.repository.ContactRepository;
import com.contactapp.contactmanager.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * UNIT test: only ContactService is real. The repositories are MOCKS (fake objects),
 * so there is no database and no Spring, and each test runs in milliseconds.
 * Every test follows: Arrange (set up) -> Act (call) -> Assert (check).
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContactService contactService;

    private static ContactRequest request() {
        return new ContactRequest("  Ali ", "Khan", "  ",
                List.of(new EmailDto(null, "ali@test.com", "work")),
                List.of(new PhoneDto(null, "03001234567", "home")));
    }

    private static Contact contact(String firstName) {
        Contact c = new Contact();
        c.setFirstName(firstName);
        c.setLastName("Khan");
        return c;
    }

    @Test
    void createSavesContactForTheUserAndCleansTheInput() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(contactRepository.save(any(Contact.class))).thenAnswer(call -> call.getArgument(0));

        ContactResponse response = contactService.create(1L, request());

        assertThat(response.firstName()).isEqualTo("Ali");   // spaces removed
        assertThat(response.title()).isNull();               // blank title becomes null
        assertThat(response.emails()).hasSize(1);
        assertThat(response.phones()).extracting(PhoneDto::phone).containsExactly("03001234567");
    }

    @Test
    void createFailsWhenUserDoesNotExist() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.create(9L, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(contactRepository, never()).save(any()); // nothing must be saved
    }

    @Test
    void listWithoutSearchUsesTheNormalQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        when(contactRepository.findByUserIdAndDeletedAtIsNull(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(contact("Ali"))));

        Page<ContactResponse> page = contactService.list(1L, "   ", pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(contactRepository, never()).search(any(), any(), any());
    }

    @Test
    void listWithSearchUsesTheSearchQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        when(contactRepository.search(1L, "ali", pageable))
                .thenReturn(new PageImpl<>(List.of(contact("Ali"))));

        Page<ContactResponse> page = contactService.list(1L, " ali ", pageable);

        assertThat(page.getContent()).extracting(ContactResponse::firstName).containsExactly("Ali");
    }

    @Test
    void getThrowsWhenContactBelongsToSomeoneElse() {
        // the query filters by user id, so for another user it returns nothing
        when(contactRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.get(2L, 5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Contact not found");
    }

    @Test
    void getHidesContactsThatAreInTheTrash() {
        Contact deleted = contact("Ali");
        deleted.setDeletedAt(LocalDateTime.now());
        when(contactRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> contactService.get(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReplacesTheDetails() {
        Contact existing = contact("Old");
        existing.addEmail(new Email("old@test.com", "home"));
        existing.addPhone(new Phone("03110000000", "work"));
        when(contactRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(existing));

        ContactResponse response = contactService.update(1L, 5L, request());

        assertThat(response.firstName()).isEqualTo("Ali");
        // the old email and phone are REPLACED, not kept next to the new ones
        assertThat(response.emails()).extracting(EmailDto::email).containsExactly("ali@test.com");
        assertThat(response.phones()).extracting(PhoneDto::phone).containsExactly("03001234567");
        verify(contactRepository).saveAndFlush(existing);
    }

    @Test
    void deleteIsASoftDelete() {
        Contact existing = contact("Ali");
        when(contactRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(existing));

        contactService.delete(1L, 5L);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(contactRepository, never()).delete(any()); // the row is NOT removed
    }

    @Test
    void listTrashReturnsDeletedContacts() {
        when(contactRepository.findByUserIdAndDeletedAtIsNotNull(1L)).thenReturn(List.of(contact("Ali")));

        assertThat(contactService.listTrash(1L)).hasSize(1);
    }

    @Test
    void restoreWorksWithin30Days() {
        Contact deleted = contact("Ali");
        deleted.setDeletedAt(LocalDateTime.now().minusDays(29));
        when(contactRepository.findForRestore(5L, 1L)).thenReturn(Optional.of(deleted));

        ContactResponse response = contactService.restore(1L, 5L);

        assertThat(response.deletedAt()).isNull();
    }

    @Test
    void restoreFailsAfter30Days() {
        Contact deleted = contact("Ali");
        deleted.setDeletedAt(LocalDateTime.now().minusDays(31));
        when(contactRepository.findForRestore(5L, 1L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> contactService.restore(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("30 days");
    }

    @Test
    void restoreFailsWhenContactIsNotInTrash() {
        when(contactRepository.findForRestore(5L, 1L)).thenReturn(Optional.of(contact("Ali")));

        assertThatThrownBy(() -> contactService.restore(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void purgeRemovesExpiredContacts() {
        List<Contact> expired = List.of(contact("A"), contact("B"));
        when(contactRepository.findByDeletedAtBefore(any())).thenReturn(expired);

        assertThat(contactService.purgeExpired()).isEqualTo(2);
        verify(contactRepository).deleteAll(expired);
    }
}
