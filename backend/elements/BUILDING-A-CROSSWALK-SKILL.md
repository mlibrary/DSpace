# Building an AI skill for updating RT2 DSpace crosswalks

Guidance for maintaining the Deep Blue ↔ Elements crosswalks with an AI coding assistant
(Claude Code, Copilot, Cursor — the shape is the same). This is the short version: the
things that actually cause failures, not a full crosswalk manual.

Everything here is enforced by one principle: **a crosswalk is two vocabularies joined by
XML, and a model will happily invent words in either.** Almost every bug we have fixed in
these files is a field name that does not exist on one side or the other. So the skill's
first job is not to edit anything — it is to go and fetch both vocabularies.

---

## 1. The skill must connect to both systems before it edits anything

Make this a hard gate: no crosswalk edit until both extracts are in hand and fresh. A
model asked to "fix the volume mapping" will otherwise guess a plausible field name, and
plausible is exactly what is wrong.

**From Elements** (secure API, `/elements-api/v6.13`):

| Fetch | Why |
|---|---|
| `GET /publication/types` then each type's fields | the only authoritative list of underlying field names, and which types carry them |
| the `choice` field values on those types | a choice field rejects anything not on its list — silently |

**From DSpace 7** (`/server/api`):

| Fetch | Why |
|---|---|
| `GET /core/metadatafields?size=2000` | the metadata registry. A field that is not here cannot be written |
| `GET /core/collections` | collection UUIDs for the deposit target |

Useful: both DSpace endpoints are readable **anonymously**. You do not need a login to
validate field names or look up a collection UUID — only to deposit.

Write both extracts to disk as TSV/CSV and have the skill diff the crosswalk against them.
That check is cheap and catches the whole class of bug. Our own run over these files found
`serie` (field is `series`), `'thesis-type` (stray apostrophe), `" c-relation-isformatof"`
(leading space) and `references` (exists on no type at all) — four dead mappings that
looked perfectly reasonable in the XML.

### Credentials

Use the same approach you already use for any service account — the point is only that the
skill reads them from a store, never from the prompt or the repo:

- **One credential set per environment, keyed by environment then system.** `dev` and
  `prod` are different Elements instances *and* different DSpace instances.
- **Never let the skill fall back across environments.** If the dev credential fails, it
  must stop and say so, not quietly use prod. Running a "dev" check against production
  data is the one mistake here with real consequences.
- Keep the credential file out of version control, and out of the model's context — have
  the skill call a script that reads it, rather than reading it itself.

---

## 2. What Elements hardcodes in this integration

This is the part that is not in the XML, and the part a model cannot infer. Treat it as
fixed ground.

**Fields Elements crosswalks automatically.** Do not map these unless you intend to
*override* the default:

```
parentCollection / parentCollectionList  ->  collections
archived / withdrawn                     ->  repository-status
dc.date.accessioned                      ->  record-created-at-source-date
dc.date.available                        ->  record-made-public-at-source-date
```

File-level metadata, including **embargo dates, is also crosswalked automatically**.
Elements' OA Monitor reads embargo state from the *file* level, never from an object-level
field — so there is no need to map `is-embargoed` or an object-level
`embargo-release-date`, and doing so does not change a compliance verdict.

**Deposit-time pseudo-fields.** These are not Elements record fields; they exist only
during a deposit and carry what the user chose on the deposit screen. They are valid only
in the deposit (XwalkOut) map:

```
depositor.full-name | .email-address | .proprietary-id
deposit.date | deposit.type                     files.count | files.names
deposit-licence.display-name | .identifier
requested-embargo.display-name | .id | .comment
requested-reuse-licence.name | .short-name | .id | .comment
object.type | object.labels | user.identifiers
oa-location-url | oa-location-file-version
```

Note `requested-embargo` has `.display-name` but `requested-reuse-licence` does **not** —
its human-readable form is `.name`. That asymmetry has caught us.

**Fixed vocabularies.** Field names, and the `data-part` names (`person:*`, `identifier:*`,
`keyword:*`, `pagination:*`, `funding-acknowledgements*`), are a closed list defined by
Elements. There is no extension mechanism. If the skill produces one that is not in the
extract, it invented it.

**DSpace 7 specifics that differ from DSpace 5/6** — relevant because older crosswalks and
older examples are full of the pre-7 forms:

- The API is **JSON**, so conditions use `select-using="jsonpath"`, not `xpath`. XPath
  arithmetic and `position()`/`last()` tricks from the old maps do not work.
- **Any metadata field not in the registry turns the whole `PUT /core/items/{id}` into a
  500** — one unregistered field fails the entire deposit, with an uninformative body.
  This is the single most common "deposit broke and the error says nothing" cause.
- An item in a submission workflow step harvests as `repository-status` =
  `Private (in review)`, where pre-7 it was `Private`.
- DSpace 7.3+ **requires a deposit licence** on every item.
- The subsequent-deposit metadata POST (re-deposit appending metadata) is a 5/6 feature.
  It does not run on 7.
- DSpace submission-form validations still apply to RT2 deposits and will fail them.

**Collection UUIDs are per-environment.** The same collection has a different UUID in dev
and prod, so the two deposit maps are maintained separately and a UUID is never copied
between them. A UUID that no longer resolves fails the deposit at workspace-item creation,
before any metadata is sent — so check it resolves (`GET /core/collections/{uuid}`) rather
than assuming.

---

## 3. Validate against the schema, every time

The XSDs are in `./schemas/` beside this file (copied from the instance's own set — the
originals stay at `rt2/dspace/schemas/` so existing tooling keeps working):

| File | Validates |
|---|---|
| `XwalkOut.xsd` | the deposit map |
| `XwalkIn.xsd` | the harvest map |
| `XwalkCommon.xsd` | shared elements, included by both |
| `XmlRelevanceScheme.xsd`, `ExternalElementsPushInfo.xsd` | related RT2 config, not the crosswalks |

Have the skill validate after every edit and refuse to hand back a file that fails.

**Know what the schema does not catch.** It checks structure — element nesting, attributes,
that a referenced value-map exists. It does **not** check that a field name is real on
either side. Those are plain strings to the XSD. So schema-valid and correct are different
claims, and the extracts from §1 are what close the gap.

One more thing it will not catch: **comments cannot contain a double hyphen**. If the skill
writes an explanatory comment mentioning `--` or a `<!-- ... -->` example, the file stops
being well-formed XML. Harmless once you know; baffling the first time.

---

## 4. Hard rules

Give the skill these as constraints it is not allowed to trade away.

1. **Never delete the template value-maps.** ISBN/ISSN character stripping, DOI extraction
   and the language-code dictionaries look like boilerplate and are not — they are what
   stops Elements rejecting imported values. A model tidying up will reach for them first.
2. **Never add `dc.description.provenance` to `field-authority-lists`.** It holds curation
   and deposit audit notes.
3. **One `<field-mapping>` per destination field.** A second mapping to the same field does
   not merge — the later one wins and the earlier one is silently dead. Add sources inside
   the existing mapping instead. We found exactly this dropping `commissioning-body`.
4. **`<xwalk:choose>` takes the first match, so order is the logic.** "book chapter" must be
   tested before "book", "image, 3-D" before "image". Never let a skill sort these.
5. **Deposit and harvest must round-trip.** Elements fetches every deposited item straight
   back through the harvest map. Every `dc.type` the deposit writes needs a matching branch
   in the harvest `object-type-selector`, or the item returns as the wrong type. Check both
   files together — a change to one is rarely complete on its own.
6. **Formats agreed between the two maps are contracts.** The deposit writes grants as
   `Grant {id}: {funder}` and the harvest parses exactly that shape back out. Change the
   prefix or separator on one side and grant IDs silently stop being recognised.
7. **OA location mappings, if used, must be exact reversals**, and the DSpace fields they
   use must be used for nothing else. Getting this wrong misreports Open Access compliance
   in the OA Monitor, which is worse than it not working at all.
8. **Choice fields take only their configured values.** Check the extract. A concatenated
   or reformatted string will never match and the field stays empty with no error.
9. **Watch for the same concept spelled two ways.** In this instance `author-licence` stores
   `CC-BY` and `publisher-licence` stores `CC BY`. A value map serving both needs both keys
   — ours had only one, so every author-set licence was being dropped.
10. **Change dev first, confirm, then mirror to prod.** Keep the two maps otherwise
    identical, so dev is actually testing what prod runs; where they must differ (collection
    UUIDs, environment URLs), say so in a comment at the point of difference.

---

## 5. A workable shape for the skill

```
1  read config: which environment, which instance, where the files are
2  fetch Elements types + fields + choice values      -> stop if auth fails
3  fetch DSpace metadata registry + collections       -> stop if unreachable
4  cross-check the current crosswalks against both    -> report every unknown name
5  make the requested change
6  validate against the XSDs                          -> refuse to return on failure
7  re-run step 4                                      -> no new unknown names
8  check deposit/harvest round-trip coverage
9  show a diff before anything is promoted or deployed
```

Steps 2–4 before 5 is the whole trick. A skill that edits first and checks after will
produce confident, well-commented, wrong XML.

Finally: **have it comment what it changed and why, in the file.** These crosswalks are
read by whoever picks them up in two years, and the non-obvious parts — embargo tokens,
licence spellings, selector ordering, the round-trip contracts — are exactly the parts that
get broken by someone making a reasonable-looking edit.
