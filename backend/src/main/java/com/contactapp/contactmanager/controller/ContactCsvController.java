package com.contactapp.contactmanager.controller;

import com.contactapp.contactmanager.dto.ImportResult;
import com.contactapp.contactmanager.exception.BadRequestException;
import com.contactapp.contactmanager.service.ContactCsvService;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Download all contacts as a CSV file, or upload a CSV file to add contacts. */
@RestController
@RequestMapping("/api/contacts")
public class ContactCsvController {

    private final ContactCsvService csvService;

    public ContactCsvController(ContactCsvService csvService) {
        this.csvService = csvService;
    }

    // The answer is a FILE, not JSON. "attachment" tells the browser to download it.
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal Jwt jwt) {
        byte[] file = csvService.exportCsv(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("contacts.csv").build().toString())
                .body(file);
    }

    // An upload is sent as "multipart/form-data"; the part called "file" arrives as a MultipartFile
    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importFile(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("The file is empty");
        }
        try (InputStream in = file.getInputStream()) {
            return csvService.importCsv(Long.valueOf(jwt.getSubject()), in);
        }
    }
}
