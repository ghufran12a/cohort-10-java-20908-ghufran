package com.contactapp.contactmanager.service;

import com.contactapp.contactmanager.dto.ContactRequest;
import com.contactapp.contactmanager.dto.EmailDto;
import com.contactapp.contactmanager.dto.ImportResult;
import com.contactapp.contactmanager.dto.PhoneDto;
import com.contactapp.contactmanager.entity.Contact;
import com.contactapp.contactmanager.exception.BadRequestException;
import com.contactapp.contactmanager.repository.ContactRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Export and import of contacts as CSV.
 * A contact has MANY phones/emails, but a CSV cell holds one text, so several values share a cell:
 *     work:03001234567; home:+923331112222
 */
@Service
public class ContactCsvService {

    private static final Logger log = LoggerFactory.getLogger(ContactCsvService.class);

    static final int MAX_ROWS = 1000;
    private static final int MAX_REPORTED_ERRORS = 20;

    private static final String FIRST_NAME = "First Name";
    private static final String LAST_NAME = "Last Name";
    private static final String TITLE = "Title";
    private static final String PHONES = "Phones";
    private static final String EMAILS = "Emails";

    // Excel runs a cell as a FORMULA when it starts with one of these ("CSV injection")
    private static final String FORMULA_STARTS = "=+-@\t\r";

    // Excel needs this invisible mark to read non-English letters (e.g. Urdu) correctly
    private static final char BOM = '﻿';

    private final ContactRepository contactRepository;
    private final ContactService contactService;
    private final Validator validator;

    public ContactCsvService(ContactRepository contactRepository, ContactService contactService,
                             Validator validator) {
        this.contactRepository = contactRepository;
        this.contactService = contactService;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(Long userId) {
        List<Contact> contacts =
                contactRepository.findByUserIdAndDeletedAtIsNullOrderByFirstNameAscLastNameAsc(userId);
        StringWriter out = new StringWriter();
        out.write(BOM);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(FIRST_NAME, LAST_NAME, TITLE, PHONES, EMAILS)
                .get();
        try (CSVPrinter printer = new CSVPrinter(out, format)) {
            for (Contact c : contacts) {
                printer.printRecord(
                        safe(c.getFirstName()),
                        safe(c.getLastName()),
                        safe(c.getTitle()),
                        safe(joinPairs(c.getPhones().stream()
                                .map(p -> pair(p.getLabel(), p.getPhone())).toList())),
                        safe(joinPairs(c.getEmails().stream()
                                .map(e -> pair(e.getLabel(), e.getEmail())).toList())));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e); // cannot happen when writing into memory
        }
        log.info("User {} exported {} contacts", userId, contacts.size());
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Good rows are saved; bad rows are skipped and explained. Every row passes the same rules as the form. */
    @Transactional
    public ImportResult importCsv(Long userId, InputStream file) {
        List<ContactRequest> rows = readRows(file);

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ContactRequest request = rows.get(i);
            Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);
            if (violations.isEmpty()) {
                contactService.create(userId, request);
                imported++;
            } else {
                skipped++;
                if (errors.size() < MAX_REPORTED_ERRORS) {
                    String reason = violations.stream().map(ConstraintViolation::getMessage)
                            .distinct().sorted().findFirst().orElse("invalid data");
                    errors.add("Row " + (i + 2) + ": " + reason); // +2: header line, and people count from 1
                }
            }
        }
        log.info("User {} imported contacts: {} saved, {} skipped", userId, imported, skipped);
        return new ImportResult(imported, skipped, errors);
    }

    private List<ContactRequest> readRows(InputStream file) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .get();
        try (Reader reader = new InputStreamReader(file, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(skipBom(reader))) {

            if (!hasHeader(parser, FIRST_NAME) || !hasHeader(parser, LAST_NAME) || !hasHeader(parser, PHONES)) {
                throw new BadRequestException(
                        "The file must have the columns: First Name, Last Name, Phones (Title and Emails are optional)");
            }
            List<ContactRequest> rows = new ArrayList<>();
            for (CSVRecord csvRow : parser) {
                if (rows.size() >= MAX_ROWS) {
                    throw new BadRequestException("A file can contain at most " + MAX_ROWS + " contacts");
                }
                rows.add(toRequest(csvRow));
            }
            if (rows.isEmpty()) {
                throw new BadRequestException("The file has no contacts");
            }
            return rows;
        } catch (IOException | UncheckedIOException | IllegalArgumentException | IllegalStateException e) {
            log.warn("Could not read uploaded CSV: {}", e.getMessage());
            throw new BadRequestException("This file could not be read as a CSV file");
        }
    }

    private static boolean hasHeader(CSVParser parser, String name) {
        return parser.getHeaderNames().stream().anyMatch(name::equalsIgnoreCase);
    }

    private static Reader skipBom(Reader reader) throws IOException {
        java.io.PushbackReader pushback = new java.io.PushbackReader(reader);
        int first = pushback.read();
        if (first != -1 && first != BOM) {
            pushback.unread(first);
        }
        return pushback;
    }

    private ContactRequest toRequest(CSVRecord row) {
        return new ContactRequest(
                cell(row, FIRST_NAME),
                cell(row, LAST_NAME),
                cell(row, TITLE),
                splitPairs(cell(row, EMAILS), (label, value) -> new EmailDto(null, value, label)),
                splitPairs(cell(row, PHONES), (label, value) -> new PhoneDto(null, value, label)));
    }

    private static String cell(CSVRecord row, String column) {
        return row.isMapped(column) && row.isSet(column) ? unsafe(row.get(column)) : "";
    }

    private static String pair(String label, String value) {
        // ':' and ';' are our separators, so they must not appear inside a label
        return label.replace(':', ' ').replace(';', ' ') + ":" + value;
    }

    private static String joinPairs(List<String> pairs) {
        return String.join("; ", pairs);
    }

    // "work:0300...; 0311..." -> two items; a value without a label gets the label "other"
    private static <T> List<T> splitPairs(String cell, BiFunction<String, String, T> create) {
        List<T> result = new ArrayList<>();
        for (String part : cell.split(";")) {
            if (part.isBlank()) {
                continue;
            }
            int colon = part.indexOf(':');
            String label = colon > 0 ? part.substring(0, colon).trim() : "other";
            String value = part.substring(colon + 1).trim();
            result.add(create.apply(label, value));
        }
        return result;
    }

    // A leading apostrophe makes Excel treat the cell as plain text instead of a formula
    private static String safe(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return FORMULA_STARTS.indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
    }

    // the reverse of safe(), so exporting and importing again gives the same text
    private static String unsafe(String value) {
        boolean escaped = value.length() > 1 && value.charAt(0) == '\''
                && FORMULA_STARTS.indexOf(value.charAt(1)) >= 0;
        return escaped ? value.substring(1) : value;
    }
}
