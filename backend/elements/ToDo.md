# Crosswalk verification checklist

## Recheck environment-specific assertions

The collection replacement was based on the wrong DSpace instance,
`backend.demo.deepblue-documents.lib.umich.edu`. Registry warnings derived from
that instance must not be presented as verified facts about the intended target
without confirmation.

- [ ] Confirm the intended staging DSpace and Elements instances and record their
  URLs and versions in the verification evidence.
- [ ] Retrieve the metadata registry and collection inventory from the correct
  staging DSpace instance.
- [ ] Check whether the target registry contains `dc.identifier.volume`,
  `dc.identifier.issue`, `dc.identifier.startpage`, `dc.identifier.endpage`,
  `dc.identifier.pmid`, and `dc.identifier.name-orcid`.
- [ ] Check the actual ISSN/eISSN and thesis degree-name/discipline fields in the
  target registry rather than relying on the demo-instance inventory.
- [ ] Confirm that the active deposit collection, `Symplectic Test Collection`
  (`601cbd93-df76-4d5e-a309-41f05ae1ebdf`), exists on the intended staging
  instance and is the approved deposit destination.
- [ ] Review the header and inline registry warnings in both
  `dspace-XwalkOut-map.xml` and `dspace-XwalkIn-map.xml`. Correct or qualify
  wrong-instance claims, including assertions about production field availability.
- [ ] Verify Elements field availability and licence/thesis choice values against
  the intended staging Elements configuration.
- [ ] Record the environment, verification date, evidence, and unresolved questions
  in the pull request; update related documentation to match confirmed findings.

## Test deposits and harvests on the correct staging instance

XSD validation checks structure and schema constraints. It does not establish
that live systems accept field names and values or that deposit/harvest round
trips preserve the intended metadata.

- [ ] Run `./backend/elements/xwalk-validate.sh` after downloading the required XSDs
  into `schemas/`.
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
- [ ] Resolve unexpected failures or explicitly document accepted limitations and
  follow-up decisions before deployment.
- [ ] Remove or retain staging test records according to the agreed test-data
  policy.
