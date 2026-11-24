package com.contactapp.contactmanager.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.contactapp.contactmanager.entity.Contact;
import com.contactapp.contactmanager.entity.Email;
import com.contactapp.contactmanager.entity.Phone;
import com.contactapp.contactmanager.entity.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * DATA ACCESS test. @DataJpaTest starts only JPA + an in-memory database, and every test is
 * rolled back afterwards, so tests never affect each other. This proves our queries really work.
 */
@DataJpaTest
class ContactRepositoryTest {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User ali;
    private User sara;

    @BeforeEach
    void setUp() {
        ali = user("Ali", "ali@test.com", null);
        sara = user("Sara", null, "03211234567");
        contact(ali, "Bilal", "Ahmed", null);
        contact(ali, "Zain", "Bilalzai", null);
        contact(ali, "Omar", "Deleted", LocalDateTime.now());
        contact(sara, "Bilal", "OfSara", null);
        entityManager.flush();
    }

    private User user(String name, String email, String phone) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash("hash");
        return entityManager.persist(u);
    }

    private Contact contact(User owner, String first, String last, LocalDateTime deletedAt) {
        Contact c = new Contact();
        c.setUser(owner);
        c.setFirstName(first);
        c.setLastName(last);
        c.setDeletedAt(deletedAt);
        c.addEmail(new Email(first.toLowerCase() + "@test.com", "work"));
        c.addPhone(new Phone("03001234567", "home"));
        return entityManager.persist(c);
    }

    private static PageRequest page(int number, int size) {
        return PageRequest.of(number, size, Sort.by("firstName"));
    }

    @Test
    void listShowsOnlyTheUsersActiveContacts() {
        Page<Contact> result = contactRepository.findByUserIdAndDeletedAtIsNull(ali.getId(), page(0, 10));

        assertThat(result.getContent()).extracting(Contact::getFirstName).containsExactly("Bilal", "Zain");
    }

    @Test
    void listIsPaginated() {
        Page<Contact> result = contactRepository.findByUserIdAndDeletedAtIsNull(ali.getId(), page(1, 1));

        assertThat(result.getContent()).extracting(Contact::getFirstName).containsExactly("Zain");
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void searchMatchesFirstOrLastNameIgnoringCaseAndOnlyForThatUser() {
        Page<Contact> result = contactRepository.search(ali.getId(), "BILAL", page(0, 10));

        // "Bilal" (first name) and "Bilalzai" (last name), but NOT Sara's Bilal
        assertThat(result.getContent()).extracting(Contact::getLastName).containsExactly("Ahmed", "Bilalzai");
    }

    @Test
    void searchIgnoresDeletedContacts() {
        assertThat(contactRepository.search(ali.getId(), "Omar", page(0, 10))).isEmpty();
    }

    @Test
    void findByIdAndUserIdHidesOtherUsersContacts() {
        Contact alisContact = contact(ali, "Secret", "One", null);

        assertThat(contactRepository.findByIdAndUserId(alisContact.getId(), ali.getId())).isPresent();
        assertThat(contactRepository.findByIdAndUserId(alisContact.getId(), sara.getId())).isEmpty();
    }

    @Test
    void trashAndExpiredQueriesWork() {
        assertThat(contactRepository.findByUserIdAndDeletedAtIsNotNull(ali.getId()))
                .extracting(Contact::getFirstName).containsExactly("Omar");
        assertThat(contactRepository.findByDeletedAtBefore(LocalDateTime.now().minusDays(30))).isEmpty();
        assertThat(contactRepository.findByDeletedAtBefore(LocalDateTime.now().plusMinutes(1))).hasSize(1);
    }

    @Test
    void userCanBeFoundByEmailOrPhone() {
        assertThat(userRepository.findByEmail("ali@test.com")).isPresent();
        assertThat(userRepository.findByPhone("03211234567")).isPresent();
        assertThat(userRepository.existsByEmail("nobody@test.com")).isFalse();
        assertThat(userRepository.existsByPhone("03211234567")).isTrue();
    }
}
