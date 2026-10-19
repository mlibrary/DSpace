# Resolving the Elements deposit-license validation error

## Goal

Make the Elements-to-DSpace integration record authorized deposit-license
acceptance in the form DSpace recognizes.

This guide is for staging. It does not authorize accepting terms on another
person's behalf or bypassing the repository's license requirement.

## What is already known

Workspace item `26` reports `error.validation.license.notgranted` at
`/sections/license`. Its license section contains:

```json
{
  "url": null,
  "acceptanceDate": null,
  "granted": false
}
```

Elements supplied deposit-license information in provenance, and the item
references an uploaded `dbdocs_touv2.1.txt` file. Neither fact proves that DSpace
recorded the acceptance it requires.

In this checkout:

- The submission definition `traditionalCC_um` includes a mandatory `license`
  step. The optional `cclicense` step is different.
- `LicenseValidation` checks for the expected license bitstream in DSpace's
  license bundle.
- `LicenseAddPatchOperation` can generate that license through the workspace API
  when `/sections/license/granted` is set to `true`.
- That operation uses DSpace's configured license text. It does not simply mark
  the uploaded Elements terms file as accepted.
- Repeating that operation removes and replaces an existing DSpace license.
  Do not repeatedly grant it as a diagnostic.

See [the campus guide](campus-step-by-step.md) for the separate missing-campus
error. Fixing one error does not fix the other.

## 1. Confirm the intended license and consent

Ask the repository owner and Elements administrator to compare:

1. The exact terms presented to the depositor in Elements.
2. The license shown for the destination collection in DSpace.
3. What consent Elements records and whether the connector is authorized to
   record the corresponding DSpace acceptance.

If the terms differ or the authority is unclear, stop and have the owners resolve
that before enabling automatic acceptance.

Do not set `granted: true` just to make an error disappear. Do not rename or move
an uploaded terms file into a license bundle as a shortcut.

## 2. Inspect the current draft

Sign into the staging frontend using an account authorized to read the draft.
In browser Developer Tools, inspect an authenticated backend request and copy
the token after `Bearer` from its `Authorization` header. The login response
may also provide the header.

Do not share the token or put it in a ticket.

In macOS zsh:

```zsh
BACKEND='https://backend.staging.deepblue-documents.lib.umich.edu/server'
WORKSPACE_ID=26
read -rs 'DSPACE_TOKEN?Paste staging token without Bearer: '; echo

curl --fail-with-body \
  -H "Authorization: Bearer $DSPACE_TOKEN" \
  "$BACKEND/api/submission/workspaceitems/$WORKSPACE_ID"
```

Confirm the draft's `sections.license` and `errors`. If you have `jq`, append this
pipe to a successful curl request to summarize the relevant fields:

```bash
| jq '{id, license: .sections.license, errors}'
```

Append the pipe to the curl command; do not run it as a separate command.

HTTP 401 means the token is missing, invalid, or expired. HTTP 403 means access
needs checking. HTTP 404 requires checking the instance, draft ID, and whether
the item has moved into workflow.

## 3. Run a controlled DSpace-side acceptance test

This distinguishes a DSpace license-processing problem from the way Elements
records acceptance.

1. Use a disposable staging draft and an account authorized to accept the
   applicable terms. Use item `26` only if the repository owner approves.
2. Open the draft through the staging submission interface.
   Service-account drafts may not appear in your own submissions list.
   If you cannot access it, ask a DSpace administrator for an authorized way to
   inspect/edit it; do not change its submitter or permissions just for this test.
3. Review the license and accept it through the DSpace license step.
   Save using the UI's normal controls. Do not attempt final submission unless
   the test plan calls for it.
4. Fetch the workspace item again using step 2.

Expected result:

- `sections.license.granted` becomes `true`.
- `error.validation.license.notgranted` is absent.
- The campus validation error may remain.

If the UI cannot save the license independently of the missing campus, use an
approved disposable draft with a correct campus, or ask the administrator/support
to run an authenticated API test. Do not disable required fields.

If the license error remains after a successful save, collect the backend error
and request details for the DSpace administrator. Do not assume Elements is the
cause in that case.

## 4. Identify the supported Elements acceptance mechanism

Send this request to the Elements administrator or Symplectic support:

> Elements 7.0.0.4562 using the DSpace JSON connector creates a draft and uploads
> files successfully. It writes the deposit-license name/ID to provenance and
> uploads a terms file, but DSpace reports error.validation.license.notgranted
> and sections.license.granted remains false. The destination uses the
> traditionalCC_um submission definition with a mandatory license step.
> Does this connector record DSpace deposit-license acceptance? What exact
> setting, supported crosswalk parameter, or connector update enables it?
> Please confirm how it associates the depositor's recorded consent with
> DSpace's configured license, and whether it successfully sends the workspace
> license-acceptance operation before creating the workflow item.

The operation supported by this DSpace checkout is a PATCH to:

```text
/server/api/submission/workspaceitems/<workspace-id>
```

with an operation shaped as:

```json
[
  {
    "op": "add",
    "path": "/sections/license/granted",
    "value": true
  }
]
```

This is reference information for the connector/support team, not a complete
curl recipe. A write request also needs appropriate authentication,
authorization, and DSpace's CSRF handling. The read-only bearer-token command
above is not sufficient as a write-request example.

Do not invent an XML parameter or treat `dc.rights`,
`dc.description.provenance`, or a Creative Commons license as equivalent to this
deposit-license operation.

## 5. Apply the confirmed integration fix

1. Back up/export the current Elements dev repository connector configuration
   and deployed crosswalk before changing them.
2. Apply the vendor-confirmed setting, parameter, or connector update.
   Modify `elements/xml/deposit-staging-patched.xml` only if the documented fix
   actually requires a change there.
3. Have the Elements administrator perform the required deployment/reload.
   Editing a local file alone does not change the hosted Elements instance.
4. Confirm that acceptance is conditional on the appropriate recorded consent,
   not automatically applied to every deposit regardless of consent.

If support confirms the connector lacks this capability, escalate for a
supported connector change. Do not remove the license step globally or add an
unreviewed automatic-acceptance customization in DSpace.

## 6. Verify a new end-to-end deposit

1. Start a new test deposit in Elements dev using a test record with a correctly
   populated campus.
2. Review and accept the approved terms through Elements' normal process.
3. Submit the deposit.
4. Verify that Elements reports success and DSpace has the expected workflow or
   archived item, depending on the collection's workflow configuration.
5. If a draft remains, inspect its `errors` and license section using step 2
   with the new workspace ID.

A completed deposit may no longer be available at the workspace-item endpoint.
A 404 by itself does not prove success: confirm the resulting workflow/item
and the Elements deposit status.

Success means the license validation error is gone and the consent/recorded
license match the approved policy. A remaining campus error means the license
fix can be verified separately, but the full deposit is not yet successful.

Where the supported test facilities allow, also verify that a deposit without
the required consent is blocked rather than automatically granted.

## 7. Evidence, rollback, and cleanup

For a failed test, collect:

- Test time and workspace/item ID.
- The request method/path and HTTP status for license acceptance, if attempted.
- Redacted response body and related backend log entries.
- Elements connector version and relevant configuration.

Do not include passwords, bearer tokens, cookies, or unnecessary personal data.
The UUID in a backend log line is a request tracing ID, not the draft's database ID.

If the integration grants the wrong license or grants without consent, stop
testing and restore the previous Elements configuration. Review affected test
items with the repository owner: configuration rollback does not undo license
records already created.

Clear the token when finished:

```zsh
unset DSPACE_TOKEN
```
