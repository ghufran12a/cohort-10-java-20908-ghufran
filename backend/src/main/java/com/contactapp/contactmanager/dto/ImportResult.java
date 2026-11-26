package com.contactapp.contactmanager.dto;

import java.util.List;

/** The report after a CSV import: how many rows worked, and why the others did not. */
public record ImportResult(int imported, int skipped, List<String> errors) {
}
