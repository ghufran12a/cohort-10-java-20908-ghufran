package com.contactapp.contactmanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * mvnw and mvnw.cmd are generated files, but this project has added some protections to them by hand.
 * Regenerating the wrapper (mvn wrapper:wrapper) silently removes those changes. These tests then FAIL,
 * so the loss is noticed and the changes can be put back (see "Maven Wrapper note" in README.md).
 * They only check that the protecting lines are still there; they do not run the scripts.
 * (Tests run from the backend folder, so the file names below are enough.)
 */
class MavenWrapperProtectionTest {

    @Test
    void unixWrapperKeepsItsLocalProtections() throws IOException {
        String script = Files.readString(Path.of("mvnw"));

        assertThat(script).contains(
                // a user name and password only over HTTPS
                "die \"MVNW_USERNAME/MVNW_PASSWORD need an https:// download address\"",
                // never for a proxy
                "if ( getRequestorType() != RequestorType.SERVER )",
                // only for the host and port of the download address
                "!\"https\".equalsIgnoreCase( asking.getProtocol() )",
                "!origin.getHost().equalsIgnoreCase( asking.getHost() ) || port( origin ) != port( asking )",
                // the checksum is for the .zip file, so unzip is required
                "die \"unzip is needed to verify and install Maven");
    }

    @Test
    void windowsWrapperKeepsItsLocalProtections() throws IOException {
        String script = Files.readString(Path.of("mvnw.cmd"));

        assertThat(script)
                .contains(
                        // a user name and password only over HTTPS, and only for the download host
                        "Write-Error \"MVNW_USERNAME/MVNW_PASSWORD need an https:// download address\"",
                        "$credentials.Add($origin, \"Basic\", $login)",
                        "$credentials.Add($origin, \"Digest\", $login)",
                        "$webclient.Credentials = $credentials",
                        // folders with ' in the name work, and a failed download stops the script
                        "Get-Content -Raw -LiteralPath $env:__MVNW_SCRIPT_PATH",
                        "break # local change")
                // the generated version offers the password to ANY host
                .doesNotContain("$webclient.Credentials = New-Object System.Net.NetworkCredential");
    }
}
