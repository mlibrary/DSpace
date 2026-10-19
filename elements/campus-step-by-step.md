# Resolving the Elements campus validation error

## Goal

Make new deposits from Elements populate DSpace's required U-M Campus field
correctly. Missing or unrecognized campus information must remain unset and fail
validation, rather than defaulting to Ann Arbor.

This guide is for staging. Do not change production while following it.

## What is already known

- Elements can create a draft and upload its files.
- Workspace item `26` in Symplectic Test Collection reports:
  `error.validation.required` at
  `/sections/traditionalpageone_um/dc.contributor.affiliationumcampus`.
- The collection UUID is `601cbd93-df76-4d5e-a309-41f05ae1ebdf`.
- Its submission definition is `traditionalCC_um`.
- DSpace's browser form supplies this metadata through the U-M Campus dropdown.
  Elements uses the API instead, so it must supply the value without that dropdown.
- The configured dropdown values are `Ann Arbor`, `Dearborn`, and `Flint`.
- `xml/deposit-staging-patched.xml` is the outbound metadata crosswalk.
  `xml/harvest-staging-patched.xml` maps data back into Elements and is not the
  place to fix missing deposit metadata.
- The attempted `depositor.primary-group-id` mapping was removed after guidance
  that it is unsupported. The example IDs `1234`, `5678`, and `9012` were never
  verified and must not be reused as real campus IDs.

The license error is separate. See [the license guide](license-step-by-step.md).

## 1. Get help with the Elements-specific part

You need someone who can inspect and change the repository integration in
Elements dev: your Elements administrator or Symplectic support.

Send this request:

> We use Elements 7.0.0.4562 with the DSpace JSON connector. Deposits to our
> staging Symplectic Test Collection fail because the required metadata field
> dc.contributor.affiliationumcampus is missing. We need a reliable source for
> the depositor's campus and the exact supported outbound crosswalk field name.
> Please confirm where that attribute is populated, whether it is exposed to
> the deposit crosswalk, and how to expose it if it is not. Do not default missing
> or unrecognized values to Ann Arbor. Please also confirm which crosswalk file
> is currently loaded in Elements dev and how to deploy and reload an update.

Do not assume that `depositor.department`, `user.department`,
`depositor.organisation`, or `depositor.campus` exists. These names have not been
verified. A user-profile or API field is not necessarily available to the deposit
crosswalk. The previously suggested "View Available Source Fields" menu has also
not been verified for this Elements version.

**Stop here if no supported source is available.** Ask support for a supported
extension/configuration approach. Changing the destination collection alone does
not populate campus.

## 2. Confirm what the source means

With the Elements administrator, establish:

1. Whether the field describes the depositor, a publication author, or another
   person. These can differ when someone deposits on another person's behalf.
2. Whether the source comes from an authoritative user feed or another maintained
   source.
3. Actual values for known Ann Arbor, Dearborn, and Flint test cases.
4. How missing values and users with multiple affiliations should be handled.

Have the repository owner approve whose campus the metadata should describe.
Do not infer campus from an unfamiliar department name or group number.

Record a small mapping table using verified values:

| Verified Elements source value | DSpace output |
|---|---|
| Actual Ann Arbor source value | `Ann Arbor` |
| Actual Dearborn source value | `Dearborn` |
| Actual Flint source value | `Flint` |
| Missing or unknown | No output; required-field validation fails |

If all three campuses cannot be tested immediately, explicitly record the
untested cases rather than treating them as verified.

## 3. Update and deploy the outbound crosswalk

1. Save a backup/export of the crosswalk currently loaded in Elements dev.
   The repository copy and the deployed copy may differ.
2. In `elements/xml/deposit-staging-patched.xml`, add a mapping inside
   `<xwalk:field-map name="default-deposit">` whose destination is
   `dc.contributor.affiliationumcampus`.
3. Use the exact source field confirmed in steps 1 and 2.
   If it already returns the DSpace labels, map it directly. Otherwise use
   the connector's supported value-mapping mechanism to translate only the
   verified values. There must be no campus fallback for missing or unknown input.
4. Ask the Elements administrator/support to validate the crosswalk using the
   connector's own validation or test facilities.
5. Check XML well-formedness from the repository root:

   ```bash
   xmllint --noout elements/xml/deposit-staging-patched.xml
   ```

   This checks XML syntax only. It does not prove that Elements supports a source
   field or that the mapping produces the right metadata.
6. Have the administrator load the updated file into Elements dev and perform
   any required reload. Confirm the loaded version and target collection.

Do not edit the XML namespace URLs. They are identifiers, not documentation
download links.

## 4. Get an authenticated staging inspection session

Use an account authorized to read the test submission, such as an authorized
administrator. In the staging frontend, sign in, open browser Developer Tools,
and inspect Network requests to the backend. Copy the token after `Bearer` from
an authenticated request's `Authorization` header. If necessary, inspect the
login response's `Authorization` header.

Do not share tokens or use a production token on staging.

In macOS zsh:

```zsh
BACKEND='https://backend.staging.deepblue-documents.lib.umich.edu/server'
read -rs 'DSPACE_TOKEN?Paste staging token without Bearer: '; echo
```

## 5. Test a new deposit

1. Use a test depositor whose source campus value was verified.
2. Make a new deposit from Elements dev. Reloading the crosswalk does not
   automatically rewrite workspace item `26`.
3. Find the new workspace item ID in Elements' result/error or the backend logs.
   A request UUID is not a workspace item ID.
4. Replace `26` below with the new numeric ID:

   ```zsh
   WORKSPACE_ID=26
   curl --fail-with-body \
     -H "Authorization: Bearer $DSPACE_TOKEN" \
     "$BACKEND/api/submission/workspaceitems/$WORKSPACE_ID"
   ```

   If the response is successful JSON, add the following pipe to summarize it
   when `jq` is installed:

   ```bash
   | jq '{
       id,
       campus: .sections.traditionalpageone_um["dc.contributor.affiliationumcampus"],
       errors
     }'
   ```

   Append the pipe to the curl command; do not run it as a separate command.

Expected result: the campus array contains the correct `value`, and `errors`
no longer contains the campus required-field error. A license error may remain;
that does not invalidate the campus test.

Repeat for each supported campus and for missing/unrecognized input. Missing or
unrecognized input must not produce an invented campus.

## 6. If the test fails

| Observation | Next action |
|---|---|
| HTTP 401 | Obtain a fresh staging access token. |
| HTTP 403 | Check access to this draft with a DSpace administrator. |
| HTTP 404 | Verify the instance and workspace ID; it may have moved into workflow. |
| Campus absent | Check the loaded crosswalk, selected field map, source availability, and source value with the Elements administrator. |
| Wrong campus | Correct the source interpretation or value map; do not bypass validation. |
| Crosswalk rejected | Ask support to identify the unsupported source or XML construct. |

Preserve redacted request/response evidence. Do not include access tokens or
unnecessary personal data in support tickets.

## 7. Completion and rollback

This issue is resolved only when new deposits produce the correct campus for
verified inputs and leave missing/unrecognized inputs unset. Save the approved
mapping in version control and retain the Elements deployment record.

If the mapping misassigns campus, restore the previous crosswalk in Elements dev
and stop further test deposits. Review any affected drafts separately; rolling
back the XML does not correct metadata already written.

Clear the token when finished:

```zsh
unset DSPACE_TOKEN
```
