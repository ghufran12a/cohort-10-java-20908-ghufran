package com.contactapp.contactmanager.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The business logic for contacts. The controller calls this; this calls the repositories.
 * Every method receives the userId, so a user can only ever touch their OWN contacts.
 */
@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    /** A deleted contact can be recovered for this many days, then it is removed for good. */
    static final long TRASH_RETENTION_DAYS = 30;

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    // Constructor injection: Spring passes in the repositories for us (no "new" needed)
    public ContactService(ContactRepository contactRepository, UserRepository userRepository) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    // @Transactional = all database steps of this method succeed together, or none are saved
    @Transactional
    public ContactResponse create(Long userId, ContactRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Contact contact = new Contact();
        contact.setUser(user);
        applyRequest(contact, request);

        Contact saved = contactRepository.save(contact);
        log.info("User {} created contact {}", userId, saved.getId());
        return ContactResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> list(Long userId, String search, Pageable pageable) {
        Page<Contact> page = (search == null || search.isBlank())
                ? contactRepository.findByUserIdAndDeletedAtIsNull(userId, pageable)
                : contactRepository.search(userId, search.trim(), pageable);
        return page.map(ContactResponse::from);
    }

    @Transactional(readOnly = true)
    public ContactResponse get(Long userId, Long contactId) {
        return ContactResponse.from(findActive(userId, contactId));
    }

    @Transactional
    public ContactResponse update(Long userId, Long contactId, ContactRequest request) {
        Contact contact = findActive(userId, contactId);
        applyRequest(contact, request);
        // flush = write the changes to the database NOW, so the new emails/phones get their ids
        // before we build the response (otherwise their id would show as null)
        contactRepository.saveAndFlush(contact);
        log.info("User {} updated contact {}", userId, contactId);
        return ContactResponse.from(contact);
    }

    /** Soft delete: the row stays, we only stamp the time. It can be recovered for 30 days. */
    @Transactional
    public void delete(Long userId, Long contactId) {
        Contact contact = findActive(userId, contactId);
        contact.setDeletedAt(LocalDateTime.now());
        log.info("User {} moved contact {} to trash", userId, contactId);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> listTrash(Long userId) {
        return contactRepository.findByUserIdAndDeletedAtIsNotNull(userId).stream()
                .map(ContactResponse::from)
                .toList();
    }

    @Transactional
    public ContactResponse restore(Long userId, Long contactId) {
        Contact contact = contactRepository.findForRestore(contactId, userId)
                .filter(c -> c.getDeletedAt() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found in trash"));

        if (contact.getDeletedAt().isBefore(LocalDateTime.now().minusDays(TRASH_RETENTION_DAYS))) {
            throw new ResourceNotFoundException("Contact was deleted more than 30 days ago and cannot be recovered");
        }
        contact.setDeletedAt(null);
        log.info("User {} restored contact {}", userId, contactId);
        return ContactResponse.from(contact);
    }

    /** Permanently removes contacts that have been in the trash longer than 30 days. */
    @Transactional
    public int purgeExpired() {
        List<Contact> expired = contactRepository
                .findByDeletedAtBefore(LocalDateTime.now().minusDays(TRASH_RETENTION_DAYS));
        contactRepository.deleteAll(expired); // emails and phones go too (cascade)
        log.info("Purged {} expired contacts from trash", expired.size());
        return expired.size();
    }

    // ---- helpers ----

    // Finds a contact only if it is THIS user's and not in the trash. Otherwise: "not found".
    // We deliberately say "not found" (not "forbidden") so nobody can discover other users' ids.
    private Contact findActive(Long userId, Long contactId) {
        return contactRepository.findByIdAndUserId(contactId, userId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
    }

    // Copies the request into the entity. Emails/phones are replaced as a whole:
    // clear() + orphanRemoval deletes the old rows, then the new ones are added.
    private void applyRequest(Contact contact, ContactRequest request) {
        contact.setFirstName(request.firstName().trim());
        contact.setLastName(request.lastName().trim());
        contact.setTitle(request.title() == null || request.title().isBlank() ? null : request.title().trim());

        contact.getEmails().clear();
        if (request.emails() != null) {
            for (EmailDto dto : request.emails()) {
                contact.addEmail(new Email(dto.email().trim(), dto.label().trim()));
            }
        }

        contact.getPhones().clear();
        for (PhoneDto dto : request.phones()) {
            contact.addPhone(new Phone(dto.phone().trim(), dto.label().trim()));
        }
    }
}
