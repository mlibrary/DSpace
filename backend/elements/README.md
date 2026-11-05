#  [Deep Blue Documents](https://deepblue.lib.umich.edu/documents) <=> [Michigan Experts](https://experts.umich.edu/) 
(DSpace <=> Symplectic Elements) crosswalks and supporting field reference files:

* `dspace-XwalkOut-map.xml` maps Elements metadata to DSpace for deposits.
* `dspace-XwalkIn-map.xml` maps DSpace metadata to Elements for harvests.
* `schemas/` contains the crosswalk and supporting configuration XSD files
  described below.
* `elements-fields.csv` lists Elements fields and publication-type values.
* `elements-legend.txt` explains the `[M]` mandatory, `[L]` locked, and `[S]`
  summary markers used in the field reference.
* `xwalk-validate.sh` validates each checked-in crosswalk XML file against
  its XSD.

## Prerequisites and reference files

* Install `xmllint` and ensure it is available on `PATH` to run the validation
commands below. 
* Download the proprietary XSD files from [Symplectic Elements Michigan Experts Google Drive folder](https://drive.google.com/drive/folders/155Ji42mKJab__ds4JaW5UlT6kIO0Sv58?usp=share_link) and place them in the `schemas/` directory.
* For guidance on building an AI-assisted crosswalk maintenance workflow, see
[Building an AI skill for updating RT2 DSpace crosswalks](BUILDING-A-CROSSWALK-SKILL.md).
The guide covers verifying live Elements and DSpace vocabularies before editing,
environment-specific credentials and collection UUIDs, schema validation,
deposit/harvest round-trip contracts, and deployment safeguards.

## Elements field reference

[`elements-fields.csv`](elements-fields.csv) is a reference grid of Elements
underlying fields by publication type. The first two rows give publication-type
display names and internal identifiers, such as `Journal Article` and
`journal-article`. Subsequent rows give an underlying field name, its data type,
and its display label and markers for each publication type. Open the CSV in a
spreadsheet to compare types or search it for an exact field name.

[`elements-legend.txt`](elements-legend.txt) explains the markers in the grid:
`[M]` means mandatory, `[L]` means locked, and `[S]` means summary. These describe
the Elements field configuration, not XML syntax or DSpace metadata requirements.

When maintaining the crosswalks, use the grid to:

* Check exact Elements field names and data types rather than using display
  labels in mappings. For example, the underlying field `series` is displayed
  as `Series`.
* Review which fields are configured for each publication type before changing
  deposit field sources or harvest field destinations. A blank cell means the
  grid lists no label for that field/type combination; verify it against the
  live configuration before relying on it.
* Identify mandatory fields when planning representative deposit/harvest tests,
  and review locked and summary fields when checking the resulting Elements
  record.
* Compare publication-type identifiers with the crosswalk selectors to review
  deposit/harvest round-trip coverage.

Treat these files as a reference snapshot, not a substitute for checking the
current Elements API. The CSV identifies choice/list field types but does not
enumerate their allowed values. Verify field availability and choice values in
the target environment, and verify DSpace fields against its metadata registry,
as described in [the crosswalk maintenance guide](BUILDING-A-CROSSWALK-SKILL.md).
XSD validation alone does not establish that either system accepts a field name
or value.

## XSD files

The `schemas/` directory contains:

* `XwalkOut.xsd`: defines mappings from Elements to a destination repository,
  including field maps and collection selection for deposits.
* `XwalkIn.xsd`: defines mappings from a source repository into Elements,
  including field maps and object-type selection for harvests.
* `XwalkCommon.xsd`: defines shared crosswalk types, field mappings, value maps,
  and selection logic. Both `XwalkOut.xsd` and `XwalkIn.xsd` import this file,
  so it must remain alongside them.
* `XmlRelevanceScheme.xsd`: defines XML relevance schemes, including category
  relevance rules, masks, aggregations, and hash definitions.
* `ExternalElementsPushInfo.xsd`: defines configuration for pushing data to an
  external Elements system, including the proprietary-ID prefix and
  per-category destination data-source settings.

## Validation

Run the crosswalk schema validation from the repository root:

```bash
./backend/elements/xwalk-validate.sh
```

The script validates `dspace-XwalkOut-map.xml` against `schemas/XwalkOut.xsd`
and `dspace-XwalkIn-map.xml` against `schemas/XwalkIn.xsd`, resolving paths
relative to the script rather than the current working directory. It exits
with a nonzero status if validation fails. `XmlRelevanceScheme.xsd` and
`ExternalElementsPushInfo.xsd` describe separate configuration documents and
are not used to validate these two crosswalks.

Validation checks XML structure and schema constraints, not compatibility with
the live Elements data model or the behavior of the RT2 crosswalk engine.

## Deposit XwalkOut mapping

* Deposit accepts both spaced (`CC BY`) and hyphenated (`CC-BY`) Creative Commons
  identifiers. Confirm these identifiers against the live Elements licence
  choices before deployment.
* Deposit accepts `Anishinaabe`, `Ojibwe`, and `Ojibwa` as labels for `oj`.
* The collection selector in the deposit XML is staging-specific and must not be
  used unchanged in production.

Before deploying, exercise representative deposits in Elements, including
embargoed items, licence choices, and each publication type.

## Harvest XwalkIn mapping

* Harvest uses the automatic creation and public-availability date mappings:
  `dc.date.accessioned` and `dc.date.available`, respectively.
* Harvest emits hyphenated Creative Commons identifiers.
* Harvest preserves all publication types emitted by the deposit crosswalk,
  including `preprint`. Existing subtype-specific selection rules retain
  precedence when an item has multiple types.
* Sponsorship entries formatted as `Grant ID: Funder` or `Grant ID` are harvested
  as grants. Other entries are preserved as acknowledgement text, with multiple
  text entries joined by newlines. Since `dc.description.sponsorship` also holds
  commissioning-body data, this convention cannot distinguish all original
  Elements funding and commissioning fields.
* `oj` has one harvest mapping, to the existing local label `Anishinaabe`;
  `yi` maps to `Yiddish`. The incorrect country-name mappings for `GE` and `PN`
  have been removed; unmatched values still pass through for the existing
  Elements validation.
* Author ORCID reconstruction is deferred pending a supported RT2 mapping
  example. Authors still come from `dc.contributor.author`, and ORCIDs from
  `dc.identifier.orcid` are harvested as record-level associated identifiers.
  `dc.identifier.name-orcid` does not currently restore author/ORCID associations.

Before deploying, exercise representative harvests in Elements, including
funding text mixed with grants.
