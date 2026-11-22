package com.contactapp.contactmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One row of the "emails" table. Many emails belong to ONE contact.
 * This is the "many" side, so it owns the foreign key column contact_id.
 */
@Entity
@Table(name = "emails")
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT creates the id
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "label", nullable = false, length = 30)
    private String label; // work, personal, ...

    // LAZY = do not load the contact from the database until someone asks for it
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false) // the foreign key column
    private Contact contact;

    // JPA (Hibernate) requires an empty constructor
    public Email() {
    }

    public Email(String email, String label) {
        this.email = email;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }
}
