package com.contactapp.contactmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.contactapp.contactmanager.entity.Contact;
import com.contactapp.contactmanager.entity.Phone;
import com.contactapp.contactmanager.entity.User;
import com.contactapp.contactmanager.repository.ContactRepository;
import com.contactapp.contactmanager.repository.UserRepository;
import com.contactapp.contactmanager.service.ContactService;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * INTEGRATION test for two things happening at the same time: a contact is being recovered while the
 * clean-up job runs. The job must wait, look again, and leave the recovered contact alone.
 * (No @Transactional here: the two sides need their own real transactions.)
 */
@SpringBootTest
class TrashLockIntegrationTest {

    @Autowired
    private ContactService contactService;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        contactRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void purgeDoesNotDeleteAContactThatIsBeingRestored() throws Exception {
        User user = new User();
        user.setName("Ali");
        user.setEmail("lock@test.com");
        user.setPasswordHash("not-used-in-this-test");
        user = userRepository.save(user);

        Contact contact = new Contact();
        contact.setUser(user);
        contact.setFirstName("Old");
        contact.setLastName("Contact");
        contact.addPhone(new Phone("03001234567", "home"));
        contact.setDeletedAt(LocalDateTime.now().minusDays(31)); // expired, so the job wants to remove it
        Long userId = user.getId();
        Long contactId = contactRepository.save(contact).getId();

        CompletableFuture<Integer> purge = new CompletableFuture<>();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            // this is the "restore" side: lock the row, then recover it
            Contact locked = contactRepository.findForRestore(contactId, userId).orElseThrow();
            locked.setDeletedAt(null);
            contactRepository.flush();

            CompletableFuture.runAsync(() -> {
                try {
                    purge.complete(contactService.purgeExpired());
                } catch (RuntimeException e) {
                    purge.completeExceptionally(e);
                }
            });
            // wait until the database itself says the purge is stuck behind us. Without this, a purge that
            // simply started late would also look like it was "waiting"
            waitForBlockedSession();
            // while the restore is not finished, the purge has to wait
            assertThatThrownBy(() -> purge.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        });

        assertThat(purge.get(10, TimeUnit.SECONDS)).isZero();
        assertThat(contactRepository.findById(contactId)).isPresent();
    }

    // H2 lists every database connection; BLOCKER_ID is filled in while one connection waits for another's lock
    private void waitForBlockedSession() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE BLOCKER_ID IS NOT NULL",
                Integer.class) == 0) {
            if (System.nanoTime() > deadline) {
                fail("the purge never waited for the restore's lock");
            }
            Thread.onSpinWait();
        }
    }
}
