package com.contactapp.contactmanager.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One row of the "contacts" table.
 * Many contacts -> one user. One contact -> many emails and many phones.
 */
@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "title", length = 100) // optional, so nullable
    private String title;

    // soft delete: null = active contact, a date = moved to trash on that date
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Many contacts belong to ONE user. This side holds the foreign key user_id.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // mappedBy = "contact" -> the foreign key lives in Email.contact, not here
    // cascade ALL     -> saving/deleting a contact also saves/deletes its emails
    // orphanRemoval   -> removing an email from this list deletes it from the database
    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Email> emails = new ArrayList<>();

    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Phone> phones = new ArrayList<>();

    // JPA (Hibernate) requires an empty constructor
    public Contact() {
    }

    // runs automatically just before the first INSERT
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Helper: keeps BOTH sides of the relationship in sync.
    // Without e.setContact(this) the foreign key contact_id would stay null and the save would fail.
    public void addEmail(Email e) {
        emails.add(e);
        e.setContact(this);
    }

    public void removeEmail(Email e) {
        emails.remove(e);
        e.setContact(null);
    }

    public void addPhone(Phone p) {
        phones.add(p);
        p.setContact(this);
    }

    public void removePhone(Phone p) {
        phones.remove(p);
        p.setContact(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Email> getEmails() {
        return emails;
    }

    public List<Phone> getPhones() {
        return phones;
    }
}
