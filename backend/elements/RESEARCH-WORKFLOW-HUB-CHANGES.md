# Crosswalk changes and staging verification

This report summarizes the mapping fixes originally compared against
`7536de95ba` (`update validation script and README to reflect recent refactoring`)
and the subsequent staging correction in `daafd21eed`
(`latest analysis against staging this time (last time it was demo)`).

Scope:

* `dspace-XwalkOut-map.xml` - deposit, from Elements to DSpace.
* `dspace-XwalkIn-map.xml` - harvest, from DSpace to Elements.

The changes combine functional fixes with extensive explanatory comments.
Much of the harvest diff is indentation-only. The functional changes below
are distinguished from newly documented behavior that already existed.

## Deposit: functional changes

| Area | Change | Effect |
|---|---|---|
| Reuse-licence provenance | Replaces `requested-embargo.display-name` with `requested-reuse-licence.name` in the reuse-licence audit paragraph. | Records the requested reuse licence instead of repeating the embargo label. |
| Associated authors | Reverses the incorrect mapping to write `associated-authors` into `dc.contributor`, with `person:lastname-firstnames` formatting. | Uses the Elements person-list as the source and a DSpace metadata field as the destination. |
| References | Comments out the `references` to `dc.identifier.citedreference` mapping. | Stops attempting to read an Elements field that the new comments report is absent from the instance's publication types. |
| Series | Changes the source from `serie` to `series`. | Uses the correct Elements underlying field name. |
| Thesis degree name | Changes the source from `'thesis-type` to `thesis-type`. | Removes a stray leading apostrophe that prevented field lookup. |
| Sponsorship | Removes the separate `commissioning-body` mapping and adds its source to the existing `dc.description.sponsorship` mapping. | Keeps commissioning-body, acknowledgement text, and grants under one destination-field mapping rather than competing mappings. |
| Exhibition type | Corrects the deposited `dc.type` from `Exhbition` to `Exhibition`. | Removes the misspelling in the type value map. |
| Name/ORCID pairs | Changes the fallback in `ignore-names-without-orcids` from `continue` to `ignore-this-value`; removes the old debug comment. | Drops entries that do not match the existing name/ORCID pattern rather than writing bare author names to `dc.identifier.name-orcid`. |
| Creative Commons licences | Adds the six hyphenated `CC-BY*` variants to both `licence-to-dc.rights` and `licence-to-dc.rights.uri`, alongside the existing spaced variants. | Produces descriptive rights statements and canonical URIs for both licence spellings. |
| Licence placeholders | Explicitly ignores `Unknown` and `Publisher's own licence` in `licence-to-dc.rights`. | Omits these placeholders instead of publishing them as rights statements. The URI map already ignored unmapped values. |
| Deposit collection | Retains `Symplectic Test Collection` (`601cbd93-df76-4d5e-a309-41f05ae1ebdf`, handle `123456789/2`). | The proposed demo collection replacement was reverted. The supplied staging inventory confirms the original collection exists on staging; the latest update retains the history in comments and removes the inactive replacement element. Production still requires its own collection UUID. |

## Harvest: functional changes

| Area | Change | Effect |
|---|---|---|
| References | Comments out the `dc.identifier.citedreference` to `references` mapping, matching the deposit change. | Stops attempting to populate the unsupported Elements field. |
| Thesis type | Removes the `dc.description.degreediscipline` source and the semicolon separator from the `thesis-type` mapping. | Sends only `dc.description.thesisdegreename`, rather than concatenating degree name and discipline into a value that would not match an Elements thesis-type choice. No replacement discipline destination is added. |

The remaining harvest changes are comments, formatting, and the addition of a
UTF-8 byte-order mark at the beginning of the file; they do not add new active
mapping rules.

## Newly documented behavior and unresolved deployment concerns

These are explanations or warnings added to the XML, not additional functional
fixes. Statements about live registries and instance configuration are reported
by the new comments; they were not independently checked against the live
systems for this review.

### Correct staging instance and metadata registry

The latest analysis targets `backend.staging.deepblue-documents.lib.umich.edu`
(DSpace 7), not the separate demo instance. Both XML headers report verification
on 2026-11-05 of a staging registry containing 303 field definitions and all
46 active deposit destination fields.

The following six fields are registered on staging. Earlier claims that they
were missing came from the wrong demo instance and have been removed from the
headers and inline mapping comments:

* `dc.identifier.volume`
* `dc.identifier.issue`
* `dc.identifier.startpage`
* `dc.identifier.endpage`
* `dc.identifier.pmid`
* `dc.identifier.name-orcid`

No registration change for these six fields is required by the supplied staging
findings. Their presence does not establish that individual records contain
values or that live deposit/harvest operations succeed.

Two staging gaps remain documented: `dc.identifier.eissn` is not registered, and
`dc.description.degreediscipline` is not registered (the actual field is
`dc.description.thesisdegreediscipline`). The duplicate ISSN/eISSN source remains
unchanged, and thesis discipline still has no harvest destination. The inactive
deposit mapping for `dc.rights.licence` would need the registered US spelling,
`dc.rights.license`, if enabled.

The updated comments cite staging metadata-field and collection TSV inventories
under `../../repository_extracts/`. These are evidence references supplied with
the analysis, not live API checks performed for this report. Retain accessible
verification evidence with the pull request.

The latest update changes comments and removes a commented-out collection
element only. Comparing the parsed XML before and after `daafd21eed`, excluding
comments and formatting, confirms that active mappings are unchanged.

### Deposit behavior

* Explains that provenance is an audit trail, not embargo or licence enforcement,
  and identifies the deposit-time pseudo-fields used to build it.
* Documents the existing `WITHHELD_<n>_MONTHS` and
  `WITHHELD_INDEFINITELY` embargo tokens, their relationship to file-level
  policies, and the need to keep Elements option labels aligned with the map.
* Explains the existing distinction between requested reuse licences,
  author licences, and publisher licences, and the rights-URI round-trip contract.
* Documents the existing `Grant {id}: {funder}` sponsorship format and its
  coupling to the harvest parsers.
* Explains first-match field-map selection, the absence of a fallback, and the
  deliberate exclusion of `c-seminar` from deposit selection.
* Warns that the enabled subsequent-deposit metadata POST path is a DSpace 5/6
  feature rather than a working DSpace 7 re-deposit mechanism. The parameter
  remains unchanged.
* Adds a comment describing a leading-space correction for
  `c-relation-isformatof`; the active source was already correctly spelled in
  the original comparison commit, so this was documentation-only in that diff.

### Harvest behavior

* Documents the existing date overrides: `dc.date.available` feeds
  `record-created-at-source-date`, and `dc.date.accessioned` feeds
  `record-made-public-at-source-date`. These still invert the automatic defaults;
  the comments request confirmation of the intended compliance-reporting behavior.
* Clarifies that folding `Private (in review)` into `Private` preserves earlier
  behavior rather than compensating for an unsupported Elements choice. The
  mapping is unchanged.
* Documents the existing sponsorship parsing, including the loss of distinction
  between commissioning-body and funder information.
* Flags that `issn` and `eissn` still read the same `dc.identifier.issn` source.
* Explains the existing `c-deep-blue` keyword scheme, licence-URI parsing, and
  limitations of journal citation construction when source fields are absent.
* Documents selector ordering and existing deposit/harvest type gaps. The new
  comments list patent, performance, artefact, design, exhibition,
  scholarly-edition, internet-publication, and fileset as falling back to
  `other`, and preprint as returning as `journal-article`. No selector branches
  are changed by this diff.

## Validation

Ran from the repository root:

```bash
./backend/elements/xwalk-validate.sh
```

Result: both current crosswalk files passed validation against their respective
XSDs in `schemas`.

Schema validation is separate from the registry and collection checks reported
by the supplied staging analysis. It does not establish that Elements choice
values are accepted or that deposit/harvest round trips behave as intended.
No live deposit or harvest was performed for this review; outstanding tests
and decisions are tracked in `ToDo.md`.
