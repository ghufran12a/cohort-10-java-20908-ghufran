package com.contactapp.contactmanager.repository;

import com.contactapp.contactmanager.entity.Contact;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for contacts. Every method takes the userId, so a user can only ever
 * reach their OWN contacts (this is the "each user sees only their own contacts" rule).
 */
public interface ContactRepository extends JpaRepository<Contact, Long> {

    // Paginated list of the user's active (not deleted) contacts.
    // Pageable carries the page number, page size and sorting.
    Page<Contact> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    // Search by first name OR last name (case-insensitive, partial match), active contacts only.
    @Query("""
            SELECT c FROM Contact c
            WHERE c.user.id = :userId
              AND c.deletedAt IS NULL
              AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Contact> search(@Param("userId") Long userId, @Param("q") String q, Pageable pageable);

    // One contact, but ONLY if it belongs to this user (used for view, update, delete).
    Optional<Contact> findByIdAndUserId(Long id, Long userId);

    // The user's trash: soft-deleted contacts that can still be recovered.
    List<Contact> findByUserIdAndDeletedAtIsNotNull(Long userId);

    // Used by the clean-up job: contacts deleted before a given date (older than 30 days).
    List<Contact> findByDeletedAtBefore(LocalDateTime cutoff);
}
