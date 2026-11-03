/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Loads the Creative Commons license CSV once at startup and exposes read-only,
 * pre-indexed views of it so callers never need to re-read or re-scan the file.
 *
 * <p>All lookups are backed by immutable collections built in
 * {@link #afterPropertiesSet()}; this bean is safe to share as a singleton.</p>
 */
public class CCLicenseCSVRepository implements InitializingBean {

    private static final Logger log = LogManager.getLogger(CCLicenseCSVRepository.class);

    /** Configuration property holding the path to the CC license CSV. */
    public static final String CSV_PATH_PROPERTY = "dspace.cc-license.csv";

    private static final String COL_CATEGORY = "CATEGORY";
    private static final String COL_VERSION = "VERSION";
    private static final String COL_UNIT = "UNIT";
    private static final String COL_JURISDICTION = "JURISDICTION";
    private static final String COL_CANONICAL_URL = "CANONICAL_URL";
    private static final String COL_IDENTIFIER = "IDENTIFIER";
    private static final String COL_TITLE = "TITLE";
    private static final String COL_ENTRY_POINT = "ENTRY_POINT";

    private static final List<String> REQUIRED_COLUMNS = List.of(
            COL_CATEGORY, COL_VERSION, COL_UNIT, COL_JURISDICTION,
            COL_CANONICAL_URL, COL_IDENTIFIER, COL_TITLE, COL_ENTRY_POINT);

    private static final String KEY_SEPARATOR = "|";

    @Autowired
    private ConfigurationService configurationService;

    // All of these are replaced with unmodifiable instances in buildIndexes().
    private List<CCLicenseCSVRow> rows = List.of();
    private Map<String, List<CCLicenseCSVRow>> byVersion = Map.of();
    private Map<String, CCLicenseCSVRow> byId = Map.of();
    private Map<String, CCLicenseCSVRow> byIdentifier = Map.of();
    private Map<String, CCLicenseCSVRow> byUri = Map.of();
    private List<CCLicenseCSVRow> entryPoints = List.of();
    private Map<String, String> licenseUriIndex = Map.of();

    @Override
    public void afterPropertiesSet() throws Exception {
        this.rows = loadCsv();
        buildIndexes();
        log.info("Loaded {} Creative Commons license rows ({} entry points, {} versions)",
                rows.size(), entryPoints.size(), byVersion.size());
    }

    // ------------------------------------------------------------------
    // Public lookup API
    // ------------------------------------------------------------------

    /**
     * @return all rows flagged as UI entry points, in CSV order (unmodifiable)
     */
    public List<CCLicenseCSVRow> getEntryPoints() {
        return entryPoints;
    }

    /**
     * Look up a row by its generated, order-dependent id ({@code "license1"}, {@code "license2"}, ...).
     *
     * <p>Prefer {@link #getByIdentifier(String)} where a stable key is needed; these ids
     * change whenever rows are reordered in the CSV.</p>
     *
     * @param id generated row id
     * @return matching row, or {@code null}
     */
    public CCLicenseCSVRow getById(String id) {
        return id == null ? null : byId.get(id.trim());
    }

    /**
     * Look up a row by the CSV {@code IDENTIFIER} column (e.g. {@code "CC BY 4.0"}, {@code "CC0 1.0"}).
     *
     * @param identifier license identifier
     * @return matching row, or {@code null}
     */
    public CCLicenseCSVRow getByIdentifier(String identifier) {
        return identifier == null ? null : byIdentifier.get(identifier.trim());
    }

    /**
     * Look up a row by its canonical URL.
     *
     * @param uri canonical license URL
     * @return matching row, or {@code null}
     */
    public CCLicenseCSVRow getByURI(String uri) {
        return uri == null ? null : byUri.get(uri.trim());
    }

    /**
     * @param version CC version (e.g. {@code "4.0"})
     * @return all rows for that version, in CSV order (unmodifiable; empty if unknown)
     */
    public List<CCLicenseCSVRow> getByVersion(String version) {
        return byVersion.getOrDefault(normalize(version), List.of());
    }

    /**
     * Resolve the canonical license URL for a version / unit / jurisdiction triple.
     *
     * @param version      CC version (e.g. {@code "4.0"})
     * @param unit         license unit (e.g. {@code "by-nc-sa"})
     * @param jurisdiction jurisdiction code (e.g. {@code "us"}), may be null or empty
     * @return canonical URL, or {@code null} if no row matches
     */
    public String findUri(String version, String unit, String jurisdiction) {
        return licenseUriIndex.get(buildKey(version, unit, jurisdiction));
    }

    /**
     * Distinct, non-blank jurisdiction codes available for a given version, in CSV order.
     *
     * @param version CC version
     * @return unmodifiable list of jurisdiction codes (empty if version is unknown or null)
     */
    public List<String> getJurisdictionsByVersion(String version) {
        return getByVersion(version).stream()
                .map(CCLicenseCSVRow::getJurisdiction)
                .filter(CCLicenseCSVRepository::isNotBlank)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    /**
     * Reads the configured CSV into a list of {@link CCLicenseCSVRow}.
     *
     * <p>Columns are located by header name, so column order in the file is irrelevant.
     * Blank lines are skipped, short rows are tolerated, and all cell values are trimmed.</p>
     *
     * @return unmodifiable list of parsed rows, in file order
     * @throws IOException            if the file cannot be read or is structurally invalid
     * @throws CsvValidationException if opencsv rejects a line
     */
    private List<CCLicenseCSVRow> loadCsv() throws IOException, CsvValidationException {
        Path csvPath = resolveCsvPath();
        List<CCLicenseCSVRow> result = new ArrayList<>();

        try (BufferedReader in = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVReader reader = new CSVReader(in)) {

            String[] header = reader.readNext();
            if (header == null) {
                throw new IOException("CC license CSV is empty: " + csvPath.toAbsolutePath());
            }
            Map<String, Integer> col = mapHeader(header, csvPath);

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (isBlankRow(row)) {
                    continue;
                }
                CCLicenseCSVRow r = new CCLicenseCSVRow();
                r.setId("license" + (result.size() + 1));
                r.setCategory(cell(row, col, COL_CATEGORY));
                r.setVersion(cell(row, col, COL_VERSION));
                r.setUnit(cell(row, col, COL_UNIT));
                r.setJurisdiction(cell(row, col, COL_JURISDICTION));
                r.setUrl(cell(row, col, COL_CANONICAL_URL));
                r.setIdentifier(cell(row, col, COL_IDENTIFIER));
                r.setTitle(cell(row, col, COL_TITLE));
                r.setEntryPoint(cell(row, col, COL_ENTRY_POINT));
                result.add(r);
            }
        }

        if (result.isEmpty()) {
            log.warn("CC license CSV {} contains a header but no data rows", csvPath.toAbsolutePath());
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Resolves and validates the CSV path from configuration. Relative paths are resolved
     * against {@code dspace.dir}.
     */
    private Path resolveCsvPath() {
        String configured = configurationService.getProperty(CSV_PATH_PROPERTY);
        if (isBlank(configured)) {
            throw new IllegalStateException(
                    "Property '" + CSV_PATH_PROPERTY + "' is not set; cannot load Creative Commons license CSV");
        }

        Path path = Paths.get(configured.trim());
        if (!path.isAbsolute()) {
            String dspaceDir = configurationService.getProperty("dspace.dir");
            if (isNotBlank(dspaceDir)) {
                path = Paths.get(dspaceDir).resolve(path);
            }
        }

        if (!Files.isReadable(path)) {
            throw new IllegalStateException(
                    "Creative Commons license CSV not found or not readable: " + path.toAbsolutePath()
                            + " (from property '" + CSV_PATH_PROPERTY + "')");
        }
        return path;
    }

    /**
     * Maps header column names to zero-based indices and verifies all required columns are present.
     *
     * <p>Header names are normalised: a UTF-8 BOM is stripped, surrounding quotes removed,
     * whitespace trimmed and the result upper-cased.</p>
     */
    private Map<String, Integer> mapHeader(String[] header, Path csvPath) throws IOException {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            String name = normalizeHeader(header[i]);
            if (!name.isEmpty()) {
                map.putIfAbsent(name, i);
            }
        }

        List<String> missing = REQUIRED_COLUMNS.stream()
                .filter(c -> !map.containsKey(c))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new IOException("CC license CSV " + csvPath.toAbsolutePath()
                    + " is missing required column(s) " + missing + "; found columns " + map.keySet());
        }
        return map;
    }

    private static String normalizeHeader(String raw) {
        String s = raw == null ? "" : raw;
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1);
        }
        return s.replace("\"", "").trim().toUpperCase(Locale.ROOT);
    }

    /** Returns the trimmed cell value, or {@code ""} if the column is absent from this row. */
    private static String cell(String[] row, Map<String, Integer> col, String column) {
        Integer idx = col.get(column);
        if (idx == null || idx >= row.length || row[idx] == null) {
            return "";
        }
        return row[idx].trim();
    }

    private static boolean isBlankRow(String[] row) {
        for (String c : row) {
            if (isNotBlank(c)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Indexing
    // ------------------------------------------------------------------

    /**
     * Builds all in-memory indexes from {@link #rows} and freezes them as unmodifiable collections.
     *
     * <ul>
     *     <li>{@code byVersion} - rows grouped by version, preserving CSV order</li>
     *     <li>{@code byId} - lookup by generated row id</li>
     *     <li>{@code byIdentifier} - lookup by CSV {@code IDENTIFIER} column</li>
     *     <li>{@code byUri} - lookup by canonical URL</li>
     *     <li>{@code entryPoints} - rows with a non-blank {@code ENTRY_POINT}</li>
     *     <li>{@code licenseUriIndex} - version|unit|jurisdiction to canonical URL</li>
     * </ul>
     *
     * <p>Duplicate keys keep the last occurrence (matching previous behaviour) and are logged.</p>
     */
    private void buildIndexes() {
        Map<String, List<CCLicenseCSVRow>> versionIdx = new LinkedHashMap<>();
        Map<String, CCLicenseCSVRow> idIdx = new HashMap<>();
        Map<String, CCLicenseCSVRow> identifierIdx = new HashMap<>();
        Map<String, CCLicenseCSVRow> uriIdx = new HashMap<>();
        Map<String, String> keyIdx = new HashMap<>();
        List<CCLicenseCSVRow> entries = new ArrayList<>();

        for (CCLicenseCSVRow row : rows) {
            versionIdx.computeIfAbsent(normalize(row.getVersion()), k -> new ArrayList<>()).add(row);

            putWarnDuplicate(idIdx, row.getId(), row, "id");
            putWarnDuplicate(identifierIdx, row.getIdentifier(), row, "identifier");
            putWarnDuplicate(uriIdx, row.getUrl(), row, "canonical URL");

            if (isNotBlank(row.getEntryPoint())) {
                entries.add(row);
            }

            String key = buildKey(row.getVersion(), row.getUnit(), row.getJurisdiction());
            String previous = keyIdx.put(key, row.getUrl());
            if (previous != null && !previous.equals(row.getUrl())) {
                log.warn("Duplicate version|unit|jurisdiction key '{}' in CC license CSV: '{}' replaced by '{}'",
                        key, previous, row.getUrl());
            }
        }

        versionIdx.replaceAll((k, v) -> Collections.unmodifiableList(v));

        this.byVersion = Collections.unmodifiableMap(versionIdx);
        this.byId = Collections.unmodifiableMap(idIdx);
        this.byIdentifier = Collections.unmodifiableMap(identifierIdx);
        this.byUri = Collections.unmodifiableMap(uriIdx);
        this.entryPoints = Collections.unmodifiableList(entries);
        this.licenseUriIndex = Collections.unmodifiableMap(keyIdx);
    }

    private static <V> void putWarnDuplicate(Map<String, V> index, String key, V value, String what) {
        if (isBlank(key)) {
            return;
        }
        V previous = index.put(key, value);
        if (previous != null) {
            log.warn("Duplicate {} '{}' in CC license CSV; the later row wins", what, key);
        }
    }

    /**
     * Builds the composite key {@code version|unit|jurisdiction}; nulls become empty strings
     * and each part is trimmed.
     */
    private static String buildKey(String version, String unit, String jurisdiction) {
        return normalize(version) + KEY_SEPARATOR + normalize(unit) + KEY_SEPARATOR + normalize(jurisdiction);
    }

    // ------------------------------------------------------------------
    // Small string helpers (avoid a commons-lang dependency in this class)
    // ------------------------------------------------------------------

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean isNotBlank(String s) {
        return !isBlank(s);
    }
}