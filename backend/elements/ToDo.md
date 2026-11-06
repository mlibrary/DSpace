# Crosswalk verification checklist

## Staging registry and collection verification

Completed checks below reflect the supplied staging analysis recorded in the
XML comments (verification date 2026-11-05), not new live checks performed while
updating this checklist. The intended DSpace target is
`backend.staging.deepblue-documents.lib.umich.edu` (DSpace 7). The earlier demo
registry warnings and collection replacement do not apply to this target.

- [x] Identify the correct staging DSpace instance and record it in both XML
  headers.
- [x] Check the staging metadata registry and collection inventory; the XML
  reports 303 field definitions and two collections.
- [x] Confirm that the staging registry contains `dc.identifier.volume`,
  `dc.identifier.issue`, `dc.identifier.startpage`, `dc.identifier.endpage`,
  `dc.identifier.pmid`, and `dc.identifier.name-orcid`, and all 46 active deposit
  destination fields.
- [x] Check the staging ISSN/eISSN and thesis fields: `dc.identifier.eissn` and
  `dc.description.degreediscipline` are absent;
  `dc.description.thesisdegreediscipline` is registered.
- [x] Confirm that the active collection, `Symplectic Test Collection`
  (`601cbd93-df76-4d5e-a309-41f05ae1ebdf`, handle `123456789/2`), exists on staging.
- [x] Remove incorrect demo-based missing-field warnings from both XML files
  and document why the demo collection replacement must not be reapplied.
- [ ] Confirm the intended staging Elements instance and record its URL and
  version alongside the DSpace environment evidence.
- [ ] Confirm that the existing staging collection is the approved test deposit
  destination and that the test account has the required access.
- [ ] Retain accessible copies or links to the staging registry and collection
  inventories cited in the XML; include the verification date in the pull request.
- [ ] Decide whether to remove the duplicate eISSN harvest mapping or register
  and populate a distinct `dc.identifier.eissn` field.
- [ ] Confirm that omission of thesis discipline from harvest is acceptable or
  agree on an Elements destination; do not concatenate it into the thesis-type
  choice value.
- [ ] If enabling the inactive `dc.rights.licence` mapping, change its target to
  the staging-registered spelling `dc.rights.license` and test it.
- [ ] Verify Elements field availability and licence/thesis choice values against
  the intended staging Elements configuration.
- [ ] Record the environment, verification date, evidence, and unresolved questions
  in the pull request; update related documentation to match confirmed findings.

## Test deposits and harvests on the correct staging instance

XSD validation checks structure and schema constraints. It does not establish
that live systems accept field names and values or that deposit/harvest round
trips preserve the intended metadata.

- [x] Run `./backend/elements/xwalk-validate.sh` against the supplied staging
  update; both maps passed. Rerun after any further XML changes.
- [ ] Use designated staging test records and confirm that no test writes target
  production.
- [ ] Test licence mappings using spaced and hyphenated Creative Commons codes,
  requested reuse licences, and placeholder values. Check deposited `dc.rights`,
  `dc.rights.uri`, provenance, and the harvested author-licence choice.
- [ ] Test authors with and without ORCIDs. Check author formatting, deposited
  `dc.identifier.orcid` and `dc.identifier.name-orcid` values, exclusion of bare
  names from name/ORCID pairs, and the documented harvest association limitations.
- [ ] Test thesis records with accepted thesis-type choices. Check the deposited
  degree name, harvested thesis type, and the documented omission of discipline
  from the thesis-type mapping.
- [ ] Test sponsorship containing free-text acknowledgements, grants with IDs and
  funders, grants without funders, and commissioning bodies. Check deposited
  repeated sponsorship values and the actual grant-ID/organization harvest
  behavior, including the loss of separate acknowledgement-text distinctions.
- [ ] Deposit a test record and confirm that it lands in the approved staging
  collection; harvest it back and verify the collection association.
- [ ] For each scenario, compare source Elements metadata, deposited DSpace
  metadata, and harvested Elements metadata. Record expected and actual results,
  test record identifiers, and any RT2 errors.
- [ ] Confirm the intended date overrides and repository-status folding, and
  test the documented publication-type and language-map limitations.
- [ ] Before production promotion, verify its registry and replace the staging
  collection UUID with the approved production destination; do not infer
  production configuration from staging.
- [ ] Resolve unexpected failures or explicitly document accepted limitations and
  follow-up decisions before deployment.
- [ ] Remove or retain staging test records according to the agreed test-data
  policy.
