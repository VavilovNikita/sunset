package com.sunsetbeach.notification;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The staff manual's list of "What's new" notes, read from the file the manual publishes ({@code updates.json},
 * the contract is in sunset-manual's README, "The feed"). The server reads it; nothing calls in. Empty
 * {@code MANUAL_UPDATES_FILE} = the feature is off.
 *
 * <p>The file belongs to another repository and is replaced while this server runs, so reading it is tolerant: a
 * missing or broken file is not an error anyone has to act on - one WARN when it stops working, one INFO when it
 * works again - and a single entry this code cannot understand is left out rather than hiding the others.
 */
@Component
public class ManualUpdateFeed {

    /** One note. The maps are keyed by language ({@code en}, {@code th}); {@code page} is relative to the manual's address. */
    public record Note(String id, LocalDate date, Map<String, String> title, Map<String, String> summary, Map<String, String> page) {
    }

    private static final Logger log = LoggerFactory.getLogger(ManualUpdateFeed.class);

    private static final long MAX_BYTES = 1 << 20;
    private static final List<String> LANGUAGES = List.of("en", "th");
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9._-]{1,80}");
    /** A path under the manual, never an address of its own: it is appended to the base URL and sent to people. */
    private static final Pattern PAGE = Pattern.compile("(?!.*\\.\\.)[A-Za-z0-9][A-Za-z0-9._/-]{0,200}");

    private final String file;
    private final Clock clock;
    private final JsonMapper json;

    private Instant lastReadAt;
    private boolean failing;

    public ManualUpdateFeed(@Value("${app.manual.updates-file:}") String file, Clock clock, JsonMapper json) {
        this.file = file == null ? "" : file.trim();
        this.clock = clock;
        this.json = json;
    }

    public boolean isConfigured() {
        return !file.isEmpty();
    }

    /** When the file was last read successfully since the server started; null if never. */
    public synchronized Instant lastReadAt() {
        return lastReadAt;
    }

    /** @return the notes as published now; empty when the feature is off or the file cannot be read. Never throws. */
    public synchronized Optional<List<Note>> read() {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            List<Note> notes = parse(bytes());
            if (failing) {
                log.info("The staff manual's update list is read again");
                failing = false;
            }
            lastReadAt = clock.instant();
            return Optional.of(notes);
        } catch (IOException | RuntimeException e) {
            if (!failing) {
                log.warn("The staff manual's update list {} cannot be read ({}: {}). It is tried again every pass and logged again when it works",
                        file, e.getClass().getSimpleName(), e.getMessage());
                failing = true;
            }
            return Optional.empty();
        }
    }

    private byte[] bytes() throws IOException {
        try (InputStream in = Files.newInputStream(Path.of(file))) {
            byte[] bytes = in.readNBytes((int) MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new IllegalStateException("larger than " + MAX_BYTES + " bytes");
            }
            return bytes;
        }
    }

    private List<Note> parse(byte[] bytes) {
        JsonNode entries = json.readTree(bytes).path("entries");
        if (!entries.isArray()) {
            throw new IllegalStateException("no \"entries\" list");
        }
        Map<String, Note> notes = new LinkedHashMap<>();
        for (JsonNode entry : entries) {
            try {
                Note note = note(entry);
                notes.putIfAbsent(note.id(), note);
            } catch (RuntimeException e) {
                log.debug("An entry of the manual's update list is left out: {}", e.getMessage());
            }
        }
        if (notes.isEmpty() && !entries.isEmpty()) {
            throw new IllegalStateException("none of its " + entries.size() + " entries can be understood");
        }
        return new ArrayList<>(notes.values());
    }

    private static Note note(JsonNode entry) {
        String id = entry.path("id").asString("");
        if (!ID.matcher(id).matches()) {
            throw new IllegalStateException("bad id");
        }
        Map<String, String> title = texts(entry.path("title"));
        Map<String, String> page = texts(entry.path("page"));
        page.values().removeIf(p -> !PAGE.matcher(p).matches());
        // A language is usable only when it has both a title to show and a page to open.
        page.keySet().retainAll(title.keySet());
        if (page.isEmpty()) {
            throw new IllegalStateException("entry " + id + " has no title with a page");
        }
        return new Note(id, LocalDate.parse(entry.path("date").asString("")), title, texts(entry.path("summary")), page);
    }

    private static Map<String, String> texts(JsonNode byLanguage) {
        Map<String, String> texts = new LinkedHashMap<>();
        for (String language : LANGUAGES) {
            String text = byLanguage.path(language).asString("").strip();
            if (!text.isEmpty()) {
                texts.put(language, text);
            }
        }
        return texts;
    }
}
