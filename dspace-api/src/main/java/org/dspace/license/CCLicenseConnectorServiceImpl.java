/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation for the Creative Commons license connector service.
 *
 * <p>This service builds Creative Commons license definitions from a local CSV
 * configuration and resolves license URIs based on user-selected field values.</p>
 *
 * <p>It does not perform any outbound HTTP calls. All license metadata is derived
 * from local configuration files:</p>
 * <ul>
 *   <li>{@code dspace.cc-license.csv} — license definitions and URL mappings</li>
 * </ul>
 *
 * <p>The service also applies business rules for mapping user selections
 * (commercial use, derivatives, jurisdiction) into CC license units.</p>
 */
public class CCLicenseConnectorServiceImpl implements CCLicenseConnectorService {

    private Logger log = org.apache.logging.log4j.LogManager.getLogger(CCLicenseConnectorServiceImpl.class);

    @Autowired
    private CCLicenseCSVRepository csvRepo;

    // Answer map keys
    private static final String FIELD_COMMERCIAL  = "commercial";
    private static final String FIELD_DERIVATIVES = "derivatives";
    private static final String FIELD_JURISDICTION = "jurisdiction";
    private static final String FIELD_SAMPLING = "sampling";

    // Sampling (recombo) answers mapped to their CC unit. The answers don't contain a '+' as it would be
    // decoded to a space in the query parameters of the REST API.
    private static final Map<String, String> SAMPLING_UNITS = Map.of(
            "sampling", "sampling",
            "samplingplus", "sampling+",
            "ncsamplingplus", "nc-sampling+"
    );

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Builds the list of available license options for the UI.
     *
     * <p>Licenses are grouped based on the CSV {@code ENTRY_POINT} column:</p>
     * <ul>
     *   <li>{@code CC_GROUP} — grouped Creative Commons license families (e.g. CC 3.0, 4.0)</li>
     *   <li>{@code RECOMBO_GROUP} — the retired Creative Commons Sampling licenses (version 1.0)</li>
     *   <li>{@code DIRECT} — standalone licenses (e.g. public domain tools)</li>
     * </ul>
     *
     * <p>CC_GROUP entries share a common license structure but differ in version and optional jurisdiction support.</p>
     *
     * @param language the current language in the ui
     *
     * @return A map of licenseId's to CCLicense
     */
    @Override
    public Map<String, CCLicense> retrieveLicenses(String language) {
        List<String> excluded = Arrays.asList(configurationService.getArrayProperty("cc.license.classfilter"));
        Map<String, CCLicense> licenses = new LinkedHashMap<>();

        for (CCLicenseCSVRow row : csvRepo.getEntryPoints()) {

            if ("CC_GROUP".equals(row.getEntryPoint())) {
                // "standard" excludes every CC version, "CC<version>" (e.g. CC3.0) excludes a single one
                if (excluded.contains("standard") || excluded.contains("CC" + row.getVersion())) {
                    continue;
                }
                boolean includeJurisdiction = "3.0".equals(row.getVersion());
                String licenseName = "4.0".equals(row.getVersion())
                        ? "Creative Commons License 4.0 International"
                        : "Creative Commons License (" + row.getVersion() + ")";

                // The row ID is used as license ID because the IDENTIFIER column contains spaces,
                // which the REST repository does not accept
                licenses.put(row.getId(),
                        buildCCLicense(
                                row.getId(),
                                licenseName,
                                includeJurisdiction
                        )
                );
            } else if ("RECOMBO_GROUP".equals(row.getEntryPoint())) {
                if (excluded.contains("recombo")) {
                    continue;
                }
                licenses.put(row.getId(), buildSamplingLicense(row.getId(),
                        "Creative Commons Sampling License (" + row.getVersion() + ")"));
            } else if ("DIRECT".equals(row.getEntryPoint())) {
                // Direct licenses are excluded individually by their unit (e.g. zero, mark, certification)
                if (excluded.contains(row.getUnit())) {
                    continue;
                }
                licenses.put(
                        row.getId(),
                        new CCLicense(
                                row.getId(),
                                row.getTitle(),
                                Collections.emptyList()
                        )
                );
            }
        }

        return licenses;
    }

    /**
     * Builds a CC license with the standard commercial/derivatives fields,
     * and optionally a jurisdiction field for 3.0.
     *
     * @param id The ID of the CC License
     * @param label The Label to user for the License
     * @param includeJurisdiction   If Jurisdiction information should be included only used for 3.0
     *
     * @return Created CCLicense
     */
    private CCLicense buildCCLicense(String id, String label, boolean includeJurisdiction) {
        CCLicenseField commercial = new CCLicenseField(
                FIELD_COMMERCIAL,
                "cc-license.commercial.label",
                "cc-license.commercial.hint",
                List.of(
                        new CCLicenseFieldEnum("y", "cc-license.commercial.option.yes.label",
                                "cc-license.commercial.option.yes.hint"),
                        new CCLicenseFieldEnum("n", "cc-license.commercial.option.no.label",
                                "cc-license.commercial.option.no.hint")
                )
        );

        CCLicenseField derivatives = new CCLicenseField(
                FIELD_DERIVATIVES,
                "cc-license.derivatives.label",
                "cc-license.derivatives.hint",
                List.of(
                        new CCLicenseFieldEnum("y", "cc-license.derivatives.option.yes.label",
                                "cc-license.derivatives.option.yes.hint"),
                        new CCLicenseFieldEnum("sa", "cc-license.derivatives.option.sharealike.label",
                                "cc-license.derivatives.option.sharealike.hint"),
                        new CCLicenseFieldEnum("n", "cc-license.derivatives.option.no.label",
                                "cc-license.derivatives.option.no.hint")
                )
        );

        if (!includeJurisdiction) {
            return new CCLicense(id, label, List.of(commercial, derivatives));
        }

        // Build jurisdiction enum from the CSV — all distinct non-blank jurisdiction
        // codes that appear under version 3.0 in the CSV.
        CCLicenseField jurisdiction = new CCLicenseField(
                FIELD_JURISDICTION,
                "cc-license.jurisdiction.label",
                "cc-license.jurisdiction.hint",
                buildJurisdictionEnums()
        );

        return new CCLicense(id, label, List.of(commercial, derivatives, jurisdiction));
    }

    /**
     * Builds the CC Sampling (recombo) license, which has a single field to choose the sampling variant.
     *
     * @param id The ID of the CC License
     * @param label The Label to use for the License
     *
     * @return Created CCLicense
     */
    private CCLicense buildSamplingLicense(String id, String label) {
        CCLicenseField sampling = new CCLicenseField(
                FIELD_SAMPLING,
                "cc-license.sampling.label",
                "cc-license.sampling.hint",
                List.of(
                        new CCLicenseFieldEnum("sampling", "cc-license.sampling.option.sampling.label",
                                "cc-license.sampling.option.sampling.hint"),
                        new CCLicenseFieldEnum("samplingplus", "cc-license.sampling.option.samplingplus.label",
                                "cc-license.sampling.option.samplingplus.hint"),
                        new CCLicenseFieldEnum("ncsamplingplus", "cc-license.sampling.option.ncsamplingplus.label",
                                "cc-license.sampling.option.ncsamplingplus.hint")
                )
        );
        return new CCLicense(id, label, List.of(sampling));
    }

    /**
     * Builds the list of jurisdiction options for the CC 3.0 license, based on {@code cc.license.jurisdiction}.
     *
     * <p>The options are aligned with {@link CreativeCommonsServiceImpl#retrieveFullAnswerMap}, which replaces the
     * submitted jurisdiction with the configured one unless it is set to {@code none}:</p>
     * <ul>
     *   <li>not set or blank — only the generic (unported) option</li>
     *   <li>{@code none} — the generic option followed by all 3.0 jurisdictions from the CSV</li>
     *   <li>any other value — only the configured jurisdiction</li>
     * </ul>
     *
     * @return Valid CCLicenseFieldEnum's
     */
    private List<CCLicenseFieldEnum> buildJurisdictionEnums() {
        String configured = StringUtils.trimToEmpty(configurationService.getProperty("cc.license.jurisdiction"));
        CCLicenseFieldEnum unported = new CCLicenseFieldEnum("", "cc-license.jurisdiction.option.unported.label", "");

        if (configured.isEmpty()) {
            return List.of(unported);
        }
        if (!"none".equals(configured)) {
            return List.of(new CCLicenseFieldEnum(configured, configured.toUpperCase(), ""));
        }

        List<CCLicenseFieldEnum> enums = new ArrayList<>();
        enums.add(unported);
        for (String code : csvRepo.getJurisdictionsByVersion("3.0")) {
            enums.add(new CCLicenseFieldEnum(code, code.toUpperCase(), ""));
        }
        return enums;
    }

    /**
     * Resolves the canonical URL for a license selection.
     *
     * <p>The license is looked up by the generated row id published by {@link #retrieveLicenses(String)}
     * (e.g. {@code license265}), <strong>not</strong> by the CSV {@code IDENTIFIER} column, which is never
     * exposed to clients. The entry row is then dispatched on the same {@code ENTRY_POINT} value used when
     * listing, so the two methods cannot disagree about a license's type:</p>
     * <ul>
     *   <li>{@code DIRECT} (CC0, PDM, certification, ...) — the entry row's own canonical URL</li>
     *   <li>{@code RECOMBO_GROUP} — the {@code sampling} answer selects the unit</li>
     *   <li>{@code CC_GROUP} — the {@code commercial} and {@code derivatives} answers are mapped to a CC unit
     *       string (e.g. {@code by-nc-sa}), then the CSV is searched for a row matching
     *       version + unit + jurisdiction; the jurisdiction answer is only used for 3.0</li>
     * </ul>
     *
     * @param licenseId The id of the given license, as published by {@link #retrieveLicenses(String)}
     * @param language The current language
     * @param answerMap The current answers
     *
     * @return The canonical URL for a license selection, or {@code null} if the id is unknown or the
     *         answers are not a valid selection for that license
     */
    @Override
    public String retrieveRightsByQuestion(String licenseId, String language,
                                           Map<String, String> answerMap) {

        CCLicenseCSVRow entry = csvRepo.getById(licenseId);

        if (entry == null) {
            log.warn("Unknown licenseId: {}", licenseId);
            return null;
        }

        String entryPoint = StringUtils.trimToEmpty(entry.getEntryPoint());

        // Standalone licenses (CC0, PDM, certification, ...): the entry row IS the license
        if ("DIRECT".equals(entryPoint)) {
            if (StringUtils.isBlank(entry.getUrl())) {
                log.warn("DIRECT license {} has no CANONICAL_URL in the CSV", licenseId);
                return null;
            }
            return entry.getUrl();
        }

        // CC Sampling licenses (recombo)
        if ("RECOMBO_GROUP".equals(entryPoint)) {
            // Map.of() based maps throw NullPointerException on get(null), so guard a missing answer
            String samplingAnswer = answerMap.get(FIELD_SAMPLING);
            String unit = samplingAnswer == null ? null : SAMPLING_UNITS.get(samplingAnswer);
            if (unit == null) {
                log.warn("Invalid sampling selection for {}: {}", licenseId, samplingAnswer);
                return null;
            }
            return csvRepo.findUri(entry.getVersion(), unit, "");
        }

        // CC licenses (3.0 / 4.0)
        if ("CC_GROUP".equals(entryPoint)) {
            String version = entry.getVersion();
            String commercial = answerMap.get(FIELD_COMMERCIAL);
            String derivatives = answerMap.get(FIELD_DERIVATIVES);

            String unit = resolveUnit(commercial, derivatives);
            if (unit == null) {
                log.warn("Invalid commercial/derivatives selection for {}: commercial={}, derivatives={}",
                        licenseId, commercial, derivatives);
                return null;
            }

            String jurisdiction = "";
            if ("3.0".equals(version)) {
                jurisdiction = answerMap.get(FIELD_JURISDICTION);
            }

            return csvRepo.findUri(version, unit, jurisdiction);
        }

        log.warn("Unhandled ENTRY_POINT '{}' for license {}", entryPoint, licenseId);
        return null;
    }

    /**
     * Maps user answers for commercial use and derivative permissions into a
     * Creative Commons license unit identifier.
     *
     * <p>This method encodes the Creative Commons license decision matrix and
     * determines the correct license component string (e.g. {@code by-nc-sa}).</p>
     *
     * <p>Only the six valid combinations are accepted. Any other value (missing, blank or
     * unknown) yields {@code null} rather than silently falling back to a more restrictive
     * license.</p>
     *
     * @param commercial the commercial answer ({@code y}|{@code n})
     * @param derivatives the derivatives answer ({@code y}|{@code sa}|{@code n})
     * @return The license usage string, or {@code null} if the combination is invalid
     */
    private String resolveUnit(String commercial, String derivatives) {
        boolean allowCommercial;
        if ("y".equals(commercial)) {
            allowCommercial = true;
        } else if ("n".equals(commercial)) {
            allowCommercial = false;
        } else {
            return null;
        }

        if ("y".equals(derivatives)) {
            return allowCommercial ? "by" : "by-nc";
        }
        if ("sa".equals(derivatives)) {
            return allowCommercial ? "by-sa" : "by-nc-sa";
        }
        if ("n".equals(derivatives)) {
            return allowCommercial ? "by-nd" : "by-nc-nd";
        }
        return null;
    }

    @Override
    public String retrieveLicenseName(final String licenseURI) {
        CCLicenseCSVRow row = csvRepo.getByURI(licenseURI);
        if (row == null) {
            log.warn("Unknown license URI: {}", licenseURI);
            return null;
        }
        return row.getTitle();
    }
}
