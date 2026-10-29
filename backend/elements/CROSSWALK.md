# Request for Symplectic Elements crosswalk review

## Requested action

Please review and test the deposit and harvest crosswalk changes on the
`xsd-validate` branch of `mlibrary/DSpace`. I do not have access to Symplectic
Elements, so I cannot verify its configured field names, licence choices, or
actual Repository Tools (RT2) processing. We need an Elements administrator or
integration owner to perform those checks before production deployment.

Both XML files have passed local XSD validation. Local static mapping and regex
checks also passed, but these do **not** establish that the crosswalks work in the
installed Elements/RT2 version or preserve metadata in a real round trip.

Please return an outcome of **approved for staging only**, **approved for
production with specified deployment configuration**, or **changes required**.
Identify incomplete tests explicitly; do not treat them as passes.

## Files and scope

| File on `xsd-validate` | Purpose |
|---|---|
| [staging-deposit-xwalkout.xml](https://github.com/mlibrary/DSpace/blob/xsd-validate/backend/elements/staging-deposit-xwalkout.xml) | Maps Elements metadata into DSpace during deposit. |
| [staging-harvest-xwalkin.xml](https://github.com/mlibrary/DSpace/blob/xsd-validate/backend/elements/staging-harvest-xwalkin.xml) | Maps DSpace metadata into Elements during harvest. |
| [README.md](https://github.com/mlibrary/DSpace/blob/xsd-validate/backend/elements/README.md) | Documents validation prerequisites, conventions, and limitations. |
| [staging-validate.sh](https://github.com/mlibrary/DSpace/blob/xsd-validate/backend/elements/staging-validate.sh) | Runs local schema validation, stopping on failure. |

Review the [branch comparison](https://github.com/mlibrary/DSpace/compare/main...xsd-validate)
and record the exact commit tested, since the branch may change.

The changes correct contributor mapping direction, field-name typos, provenance,
ORCID filtering, date mapping, licence aliases and URL patterns, publication-type
selection, funding interpretation, and language values. Author-level ORCID
reconstruction and exact CC licence-version preservation remain unresolved.

## Safe staging setup

1. Back up the currently active crosswalks and relevant repository configuration.
   Record the Elements and RT2 versions and repository connection being tested.
2. Coordinate a controlled test window with the DSpace owner. Use staging
   connections and synthetic or approved test records, not production records.
   Avoid triggering a broad harvest or deposit.
3. Verify the deposit collection selector. It currently selects **Symplectic Test
   Collection**, UUID `601cbd93-df76-4d5e-a309-41f05ae1ebdf`. Confirm that UUID
   resolves to the intended staging collection. A production target must be
   configured separately.
4. Load the proposed files using the supported procedure for your installation.
   Record reload/restart requirements and check configuration-load errors before
   processing records. No particular Elements administration screen is assumed.
5. Capture input metadata and mapped output where tooling permits. Arrange
   DSpace-side metadata inspection with the DSpace owner; screenshots alone may
   hide incorrect identifiers or missing values.
6. Restore the previous configuration after testing unless the staging changes
   are explicitly approved to remain active. Retain rollback copies.

## Confirm the local Elements model

| Area | Confirmation needed |
|---|---|
| Fields | `associated-authors`, `series`, `thesis-type`, and `c-relation-isformatof` exist with the expected types. Verify `series` especially; its spelling was inferred from the existing harvest mapping. |
| Names | `associated-authors` supports `person:lastname-firstnames` formatting with the configured `, ` delimiter. |
| Licence choices | Confirm internal identifiers and display/short names for `author-licence`, `publisher-licence`, and `requested-reuse-licence`. Deposit accepts both spaced `CC BY` and hyphenated `CC-BY` families; harvest emits hyphenated choices. |
| Provenance fields | `requested-reuse-licence.display-name`, `.id`, and `.comment` resolve in the deposit context. |
| Publication types | The selected type identifiers are enabled under category `publication`, particularly `preprint`, `fileset`, and newly restored types. |
| Automatic dates | Without explicit overrides, this RT2 version maps `dc.date.accessioned` to creation and `dc.date.available` to public availability. |
| Languages | `Anishinaabe` and `Yiddish` are accepted values. |
| Engine processing | Sponsorship filters apply before nested grant processing; multiline acknowledgements are preserved. Existing case-insensitive JSONPath type filters work, including multiple `dc.type` values. |

## Required staging tests

Record actual input and output values, not just whether processing completed.
Include a normal title, author, editor, DOI, and publication date as regression
controls.

### 1. Corrected deposit fields

| Elements input | Expected DSpace result |
|---|---|
| Associated author with separate family/given names | A formatted name in `dc.contributor`, not a destination field named `associated-authors`. |
| Series | Value in `dc.relation.ispartofseries`. |
| Thesis type | Value in `dc.description.thesisdegreename`. |
| `c-relation-isformatof` | Value in `dc.relation.isformatof`. |
| Distinct embargo and reuse-licence display names | Reuse-licence provenance contains the licence name, not the embargo name; embargo provenance remains correct. |
| Type `exhibition` | `dc.type` is spelled `Exhibition`. |

### 2. Author ORCIDs

Deposit a record with an author having an ORCID and another without one.
Where feasible, include an ORCID ending in `X` and exercise both resolved-user
and record-metadata ORCID sources using approved test identifiers.

Verify that both authors remain in `dc.contributor.author`, but only the author
with an ORCID appears in `dc.identifier.name-orcid`, formatted as
`Family, Given; ORCID`. Confirm that resolved-user ORCID precedence is unchanged.

**Deferred limitation:** harvest still builds authors from `dc.contributor.author`
and imports `dc.identifier.orcid` as record-level associated identifiers. It does
not reconstruct author associations from `dc.identifier.name-orcid`. Please
provide a supported RT2 mapping example or vendor guidance if this is required.
Do not approve the branch as having solved author-level ORCID round trips.

### 3. Dates, embargoes, and subsequent deposits

Harvest a staging item with distinct accession and availability dates, including
an embargoed example if possible. Verify creation corresponds to accession and
public availability corresponds to availability. Inspect file-level access and
embargo information too; a correct date alone does not prove access enforcement.

Submit an additional file to an existing staging item. Compare metadata before
and after: the `subsequent-deposit` mapping should append provenance without
replacing existing rights, authors, or other descriptive metadata.

### 4. Licence mappings

Test all six CC 4.0 families:

| Family | Expected deposit URI |
|---|---|
| CC BY | `http://creativecommons.org/licenses/by/4.0/` |
| CC BY-SA | `http://creativecommons.org/licenses/by-sa/4.0/` |
| CC BY-ND | `http://creativecommons.org/licenses/by-nd/4.0/` |
| CC BY-NC | `http://creativecommons.org/licenses/by-nc/4.0/` |
| CC BY-NC-SA | `http://creativecommons.org/licenses/by-nc-sa/4.0/` |
| CC BY-NC-ND | `http://creativecommons.org/licenses/by-nc-nd/4.0/` |

Confirm the intended description in `dc.rights` and URI in `dc.rights.uri`.
Where actual inputs support both identifier spellings, verify equivalent output
for `CC BY` and `CC-BY` families. Do not change the live vocabulary just to
manufacture those inputs. Harvest HTTP and HTTPS licence URLs and confirm the
intended generic Elements choices.

Also test `https://www.gnu.org/licenses/gpl.html`, version-specific GPL URLs
(including extensionless forms emitted by deposit), and
`http://creativecommons.org/licenses/publicdomain/`. Confirm they select the
intended choices without processing errors.

### 5. Licence versions: explicit decision required

The existing design loses version information:

`CC BY 3.0 URI in DSpace -> CC-BY in Elements -> CC BY 4.0 URI on a later initial deposit`

Jurisdiction information can also be lost. This branch's alias fixes do not
resolve that issue. Harvest alone does not change the source DSpace licence,
and subsequent deposits only append provenance through this field map. The risk
arises when another initial deposit or other process regenerates rights metadata
from a generic Elements choice.

Inspect older and jurisdiction-specific CC URIs using staging copies. Do not
interpret a generic licence family as authorization to upgrade an existing
licence. Please report whether Elements supports version-specific choices or a
field preserving the original URI, and whether RT2 can prefer that URI on deposit.

Until a preservation strategy is verified, retain existing DSpace licence URIs
and do not bulk-normalize them to 4.0. Production approval must explicitly
acknowledge this limitation or identify a required follow-up fix.

### 6. Publication-type round trips

For each supported type, deposit into staging, inspect `dc.type`, then harvest
and confirm the selected Elements type:

| Elements type | DSpace `dc.type` | Elements type | DSpace `dc.type` |
|---|---|---|---|
| `book` | Book | `chapter` | Book Chapter |
| `dataset` | Dataset | `conference` | Conference Paper |
| `journal-article` | Article | `patent` | Patent |
| `report` | Report | `software` | Software |
| `performance` | Performance | `artefact` | Artefact |
| `design` | Design | `exhibition` | Exhibition |
| `scholarly-edition` | Scholarly edition | `thesis-dissertation` | Thesis |
| `other` | Other | `poster` | Poster |
| `composition` | Musical Score | `internet-publication` | Internet publication |
| `figure` | Map | `fileset` | Fileset |
| `media` | Media | `presentation` | Presentation |
| `preprint` | Preprint | | |

Report unavailable local types instead of silently substituting `other`.
Verify `Book Chapter` selects `chapter`, not `book`; an item with both `Article`
and `Patent` retains article precedence; an unknown type retains the fallback to
`other`.

### 7. Funding text and grants

Harvest separate sponsorship values with these expectations:

| `dc.description.sponsorship` input | Expected interpretation |
|---|---|
| `Supported by Foundation X.` | Acknowledgement text, not a grant organization. |
| `Grant 123: Foundation X` | Grant ID `123`, organization `Foundation X`. |
| `Grant 456` | Grant ID `456`, without an invented organization. |
| A second free-text value containing a line break | Text retained alongside other acknowledgements, joined with newlines. |

Deposit the harvested record into a separate test item and inspect its funding
values. Also inspect `commissioning-body`, since it shares the sponsorship
field. The crosswalk cannot recover every value's original meaning. Confirm this
convention matches local policy, especially for organization-only values or free
text beginning with `Grant`.

### 8. Languages

Verify `yi` harvests to `Yiddish` and `oj` to the accepted local label
`Anishinaabe`. Where supported, confirm `Anishinaabe`, `Ojibwe`, and `Ojibwa`
deposit to `oj`.

Incorrect mappings from `GE` to `Georgia` and `PN` to `Pitcairn Islands` were
removed. Unknown values still pass through; check whether Elements rejects or
flags these appropriately rather than treating them as valid languages.

## Creative Commons API shutdown: separate operational task

[Creative Commons announced](https://opensource.creativecommons.org/blog/entries/2026-05-06-sunsetting-api/)
that `api.creativecommons.org` will terminate on **November 2, 2026**, and may
become unavailable earlier.

These crosswalks use local lookup tables and do not call that API. URLs under
`creativecommons.org/licenses/` identify legal terms, not the retiring service.
The legacy interactive DSpace submission CC chooser is a separate concern that
this branch does not replace.

The DSpace owner should handle that independently, for example by disabling the
dynamic step and providing an approved static `dc.rights.uri` dropdown. Exact
CC 4.0 URIs are appropriate for new licensing decisions, not automatic replacement
of older licences. Distinguish chooser availability from crosswalk processing
when reporting results.

## Results to return

Return the completed table and any required configuration changes. Share only
synthetic or redacted metadata and logs through approved project channels.
Exclude credentials, personal contact details, and unrelated record data.

| Review detail | Result |
|---|---|
| Exact branch commit tested | |
| Elements and RT2 versions | |
| Staging connection and collection verified | |
| Configuration loaded without errors | |
| Field identifiers and choice values confirmed | |
| Corrected deposit fields and provenance | Pass / Fail / Not tested; evidence reference |
| ORCID filtering and acknowledged reconstruction limitation | Pass / Fail / Not tested; evidence reference |
| Dates, embargo observations, and subsequent deposit | Pass / Fail / Not tested; evidence reference |
| CC and other licence mappings | Pass / Fail / Not tested; evidence reference |
| Older/jurisdiction-specific licence handling and follow-up decision | |
| Publication types and precedence | Pass / Fail / Not tested; list exceptions |
| Funding and commissioning-body policy | Pass / Fail / Not tested; evidence reference |
| Languages and unknown-value behavior | Pass / Fail / Not tested; evidence reference |
| Previous configuration restored or staging retention approved | |
| Approval recommendation and unresolved blockers | |

For failures, include the test record identifier, exact input, expected and actual
output, processing direction, and relevant redacted error message.
Production rollout requires an approved production collection target, a rollback
plan, and resolution or explicit acceptance of the documented limitations.