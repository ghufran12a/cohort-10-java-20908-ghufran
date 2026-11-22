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
 * @RestController = "every method returns data (JSON), not an HTML page"
 * @RequestMapping = the base url for every method in this class
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    // TEMPORARY: until login exists, every request acts as user 1 (the dev user).
    // In the security phase this is replaced by the id of the logged-in user (from the JWT).
    private static final Long TEMP_USER_ID = 1L;

    private static final int MAX_PAGE_SIZE = 50;

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    // POST /api/contacts  -> 201 Created
    // @RequestBody = turn the JSON in the request into a ContactRequest object
    // @Valid       = check the validation rules first; if they fail, our code never runs (400)
    @PostMapping
    public ResponseEntity<ContactResponse> create(@Valid @RequestBody ContactRequest request) {
        ContactResponse created = contactService.create(TEMP_USER_ID, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/contacts?search=ali&page=0&size=10  -> 200 OK
    @GetMapping
    public PageResponse<ContactResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // never trust the client: keep page >= 0 and 1 <= size <= 50, and fix the sort order ourselves
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("firstName", "lastName"));

        return PageResponse.from(contactService.list(TEMP_USER_ID, search, pageable));
    }

    // GET /api/contacts/trash  -> the user's deleted contacts (can be recovered for 30 days)
    @GetMapping("/trash")
    public List<ContactResponse> trash() {
        return contactService.listTrash(TEMP_USER_ID);
    }

    // GET /api/contacts/5  -> one contact's full profile
    // @PathVariable = take the {id} from the url
    @GetMapping("/{id}")
    public ContactResponse get(@PathVariable Long id) {
        return contactService.get(TEMP_USER_ID, id);
    }

    // PUT /api/contacts/5  -> update (replace) the contact
    @PutMapping("/{id}")
    public ContactResponse update(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return contactService.update(TEMP_USER_ID, id, request);
    }

    // DELETE /api/contacts/5  -> 204 No Content (soft delete: moves it to the trash)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contactService.delete(TEMP_USER_ID, id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/contacts/5/restore  -> take the contact out of the trash
    @PostMapping("/{id}/restore")
    public ContactResponse restore(@PathVariable Long id) {
        return contactService.restore(TEMP_USER_ID, id);
    }
}
