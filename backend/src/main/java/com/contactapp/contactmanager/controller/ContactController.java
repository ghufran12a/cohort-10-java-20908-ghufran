package com.contactapp.contactmanager.controller;

import com.contactapp.contactmanager.dto.ContactRequest;
import com.contactapp.contactmanager.dto.ContactResponse;
import com.contactapp.contactmanager.dto.PageResponse;
import com.contactapp.contactmanager.service.ContactService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The REST API for contacts. It only deals with HTTP (urls, methods, status codes, JSON).
 * All real work is delegated to ContactService.
 *
 * Every method needs a valid login token. WHO is calling comes from the token
 * (@AuthenticationPrincipal Jwt), never from something the client types, so a user can
 * never ask for another user's contacts.
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    // POST /api/contacts  -> 201 Created
    // @RequestBody = turn the JSON in the request into a ContactRequest object
    // @Valid       = check the validation rules first; if they fail, our code never runs (400)
    @PostMapping
    public ResponseEntity<ContactResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody ContactRequest request) {
        ContactResponse created = contactService.create(userId(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/contacts?search=ali&page=0&size=10  -> 200 OK
    @GetMapping
    public PageResponse<ContactResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // never trust the client: keep page >= 0 and 1 <= size <= 50, and fix the sort order ourselves
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("firstName", "lastName"));

        return PageResponse.from(contactService.list(userId(jwt), search, pageable));
    }

    // GET /api/contacts/trash  -> the user's deleted contacts (can be recovered for 30 days)
    @GetMapping("/trash")
    public List<ContactResponse> trash(@AuthenticationPrincipal Jwt jwt) {
        return contactService.listTrash(userId(jwt));
    }

    // GET /api/contacts/5  -> one contact's full profile
    // @PathVariable = take the {id} from the url
    @GetMapping("/{id}")
    public ContactResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return contactService.get(userId(jwt), id);
    }

    // PUT /api/contacts/5  -> update (replace) the contact
    @PutMapping("/{id}")
    public ContactResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody ContactRequest request) {
        return contactService.update(userId(jwt), id, request);
    }

    // DELETE /api/contacts/5  -> 204 No Content (soft delete: moves it to the trash)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        contactService.delete(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/contacts/5/restore  -> take the contact out of the trash
    @PostMapping("/{id}/restore")
    public ContactResponse restore(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return contactService.restore(userId(jwt), id);
    }

    // The token's "subject" is the user id we put there at login
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
