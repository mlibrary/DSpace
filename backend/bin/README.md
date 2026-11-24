# `backend/bin` — Scripts and Utilities

Operational scripts, cron-job helpers, and supporting data files for the
Deep Blue Documents (DSpace) repository backend. Scripts are written primarily
in Perl, with a small number in Ruby, Shell, and XSLT. Most Perl scripts read
database credentials and path information from environment variables
(`DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SERVICE`, `DB_PORT`, `BASE_DATA_DIR`,
etc.) so they can be run in both development and production containers without
hard-coded secrets.

> **Note:** The `sheets/` subdirectory contains CSV inventory files used during
> analysis and documentation; those files are not operational scripts, are
> excluded from this reference, and are not tracked in version control.

---

## Kubernetes Cron Wrapper Contract

The job-specific Bash wrappers in `cronjobs/wrappers/` are intended to be called
by Kubernetes CronJobs, for example:
`bash /dspace/bin/cronjobs/wrappers/checksum-checker.sh production`.
They set `CRON_WRAPPER=1` and `CRON_ENVIRONMENT` for the wrapped script.
Deploy these script updates in the backend image before enabling the wrappers.

In wrapper mode, `check-checksum`, `report-double-original`,
`report-too-many-authors`, `email-aptrust-bagging-errors`, and
`report-aptrust-status` exit **10** when they find anomalies. The author and
bagging-error scripts print their findings instead of sending their own email.
The APTrust status check retains the existing monitoring policy that any
non-`Success` result warrants attention, including work still in progress.
The wrapper logs the output and sends one environment-labeled notification.
Without wrapper mode, the existing direct-invocation notification and exit
behavior is preserved; checksum results are now correctly included in output.

`filter-media-cronjob` and `move-to-aptrust-regular` exit nonzero in wrapper
mode for their detected per-item failures. The mover also checks replicator
and AWS upload exit status and does not record a failed upload as sent.
This is not a comprehensive audit of the legacy bag-building utilities:
some auxiliary subprocesses still do not check exit status.
`report-about-dearborn-items` keeps delivering its attachment with its original,
unprefixed subject, and fails if delivery fails.
`perl_mailer` now fails explicitly on delivery errors.

Each job wrapper accepts exactly one environment argument: `production`,
`staging`, `demo`, `workshop`, `preview`, or `testing`. Production is enabled
for all jobs; non-production environments are restricted to the jobs documented
below.
Wrappers share `_runner.sh` for environment validation, output capture,
notifications, and exit handling.

Run the wrapper tests with
`bash backend/bin/cronjobs/wrappers/tests/wrappers.sh` and the isolated script
contract checks with `perl backend/bin/tests/cron-wrapper-contract.t` from the
repository root. The contract checks mock database, HTTP, and email
dependencies, and run the APTrust mover only in dry-run mode.

---

## Top-Level Scripts and Templates

Utility scripts and XML metadata templates located directly in `backend/bin/`.

| File                                           | Type         | Description                                                                                                                                                                                                                                                                                                                                                                                                     |
|------------------------------------------------|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`additonal_stats_data`](additonal_stats_data) | Perl script  | Ingests additional per-item statistics data into the custom `statsdata` table; clears the table first, then repopulates it by iterating all item handles and recording authors, title, date added, publisher, bitstream count, and owning collection UUID for each archived item.                                                                                                                               |
| [`consolidate_ips`](consolidate_ips)           | Perl script  | Consolidates per-request IP-based usage records into monthly, per-item summary tables. Accepts `-d YYYY/MM` and connects to PostgreSQL via environment variables. After consolidation, removes the auxiliary `BitstreamIPStatsData` and `ItemIPStatsData` rows and rebuilds `statsidanddate`.                                                                                                                   |
| [`embargo-item`](embargo-item)                 | Perl script  | CLI helper to apply or modify embargoes via the custom `umrestricted` table. Accepts `-h <handle>` and `-d <YYYY-MM-DD>` to set an item's embargo release date. Validates the date format; inserts a new row or updates an existing one.                                                                                                                                                                        |
| [`find_crawlers`](find_crawlers)               | Perl script  | Parses DSpace `dspace.log.*` files from the directory supplied by `-l <logDir>`, for the month given by `-d YYYY/MM`; stores all item- and bitstream-view IP events in the `crawlerip` table, then resolves hostnames for IPs with fewer than 300 bitstream downloads via the `all_ips` DB table or `nslookup`. [³]                                                                                             |
| [`ip_stats_data`](ip_stats_data)               | Perl script  | Ingests DSpace application log files and writes per-access records into two custom statistics tables (`BitstreamIPstatsdata` and `ItemIPstatsdata`). Accepts `-d YYYY/MM` and `-l <logDir>`. Classifies each IP as University of Michigan or external using a hardcoded list of UM subnet prefixes.                                                                                                             |
| [`meta_file`](meta_file)                       | XML template | Parameterised Dublin Core XML snippet with `$HANDLE` and `$METAVAKYE` placeholders used by batch scripts to set `dc.identifier.uri` and `dc.type` on items. [¹]                                                                                                                                                                                                                                                 |
| [`meta_file_delete`](meta_file_delete)         | XML template | Dublin Core XML fragment used as a template for the `itemupdate` command to target an item by handle; contains only a `$HANDLE` placeholder for `dc.identifier.uri`. Used when the target field is specified as a separate `itemupdate` argument, not in the template itself.                                                                                                                                   |
| [`meta_file_orcid`](meta_file_orcid)           | XML template | Minimal Dublin Core XML snippet with `$HANDLE` and `$METAVAKYE` placeholders for setting `dc.identifier.uri` and `dc.identifier.name-orcid` on items via `itemupdate`. [¹] [²]                                                                                                                                                                                                                                  |
| [`monthly_report`](monthly_report)             | Perl script  | Connects to the DSpace PostgreSQL database and prints a monthly growth report to STDOUT with two sections — items and bitstreams — showing counts for the reporting month, previous month, FYTD, same month last year, FYTD last year, and year-over-year percentage difference. Takes no arguments; derives dates from the current system clock.                                                               |
| [`prep-logs`](prep-logs)                       | Perl script  | Monthly stats pipeline orchestrator. Reads `BASE_LOG_DIR` and `BASE_BIN_DIR` environment variables; purges log files and empty directories older than 4 months from `/dspace/data/log`; extracts `view_`-event lines from `dspace.log-*` into a monthly staging subdirectory; then sequentially invokes `find_crawlers`, `remove_ips`, `additonal_stats_data`, `ip_stats_data`, and `consolidate_ips`. [4]      |
| [`remove_ips`](remove_ips)                     | Perl script  | Log pre-processor that strips known crawler/bot IP lines from DSpace access logs before stats ingestion. Accepts `-d YYYY/MM` and `-l <logDir>`. Also reads `BASE_LOG_DIR` to locate a temporary work file. Crawler IPs come from three sources: high-volume IPs in `crawlerip`, IPs with known-bot hostnames in `crawlerip.site`, and the `crawlers_dspace` table. Overwrites the original log files in place. |
| [`update_all_ip_table`](update_all_ip_table)   | Perl script  | Queries `all_ips` for IPs with an empty hostname recorded against the previous month, performs a reverse DNS lookup (`nslookup`, 5-second timeout) for each, and updates `crawlerip.site` with the resolved hostname. [5]                                                                                                                                                                                       |

---

## `aptrust/` — APTrust Preservation

Scripts and supporting files for packaging and uploading items to the
APTrust digital-preservation service.

| File                                                                   | Type                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|------------------------------------------------------------------------|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`email-aptrust-bagging-errors`](aptrust/email-aptrust-bagging-errors) | Perl script         | Monitoring/alerting tool for APTrust preservation workflows. Counts rows in `aptrust_bagging_error`; if any exist, emails a fixed alert address (`ulib-deepblue-documents-cron-reporting@umich.edu`). Also queries `aptrust_object_status` for non-Success items and sends a second alert if any are found. Mail server from `mail__P__server` env var.                                                                                                                                   |
| [`metadata2aptrust-info.xsl`](aptrust/metadata2aptrust-info.xsl)       | XSLT 1.0 stylesheet | Transforms a `<metadata>` XML document into the plain-text `aptrust-info.txt` bag metadata file. Outputs the item title plus hardcoded `Access: Institution` and `Storage-Option: Glacier-Deep-OR` lines. Used during bag creation for preservation export.                                                                                                                                                                                                                               |
| [`metadata2bag-info.xsl`](aptrust/metadata2bag-info.xsl)               | XSLT 2.0 stylesheet | Transforms a DSpace item XML representation into a plain-text BagIt `bag-info.txt` file, containing `Source-Organization`, today's `Bagging-Date`, `Bag-Count: 1 of 1`, and the DSpace handle (`2027.42/…`) as `Internal-Sender-Identifier`.                                                                                                                                                                                                                                              |
| [`move-to-aptrust-regular`](aptrust/move-to-aptrust-regular)           | Perl script         | Orchestrates the regular APTrust export pipeline: selects new, modified, and previously-errored items from the database; invokes the DSpace replicator (`dspace curate -t transmitaip`) to create BagIt bags; validates bitstream checksums; then uploads each `.tar` bag to APTrust via the AWS S3 CLI (`aws s3 cp`) and records the result in `aptrust_bags_sent`. [6]                                                                                                                  |
| [`report-aptrust-status`](aptrust/report-aptrust-status)               | Perl script         | Queries `aptrust_bags_sent` for items with `check_status='CHECK'` and items in `aptrust_object_status` with non-Success status; calls the APTrust Pharos REST API for each (using `APTRUST_API_USER_PROD`, `APTRUST_API_KEY_PROD`, `APTRUST_API_URL_PROD` env vars) and updates `aptrust_object_status` with the ingest result. The `-t` and `-o` command-line options are present but immediately overridden by hardcoded values and have no effect at runtime. Does not send email. [7] |
| [`saxon9he.jar`](aptrust/saxon9he.jar)                                 | Java library (JAR)  | Saxon-HE XSLT processor JAR file. Required at runtime by scripts that invoke XSLT 2.0 transformations (e.g., `metadata2bag-info.xsl`). Not a standalone script — invoked via `java -jar`.                                                                                                                                                                                                                                                                                                 |

---

## `cronjobs/` — Scheduled Tasks

Scripts intended to run as recurring cron jobs: integrity checks, embargo
releases, filter-media runs, and routine reports.

| File                                                                        | Type         | Description                                                                                                                                                                                                                                                                                         |
|-----------------------------------------------------------------------------|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`check-checksum`](cronjobs/check-checksum)                                 | Perl script  | Connects to the DSpace PostgreSQL database and queries `checksum_history` for any result codes other than `CHECKSUM_MATCH`, `BITSTREAM_MARKED_DELETED`, and `BITSTREAM_NOT_FOUND`; prints a report of any anomalous results for manual review.                                                      |
| [`filter-media-cronjob`](cronjobs/filter-media-cronjob)                     | Perl script  | Selects up to 5,000 archived items that have at least one PDF in their ORIGINAL bundle but no TEXT bundle (i.e., items awaiting text extraction), and runs `dspace filter-media -i <handle>` for each to trigger thumbnail generation and text extraction. [8]                                      |
| [`find-items-to-unrestrict-bio`](cronjobs/find-items-to-unrestrict-bio)     | Perl script  | Queries the Bio Station collection (hardcoded UUID `2ede3a84-…`) for items whose `dc.date.issued` year equals the current year minus 75, and prints a list of their handles to STDOUT for manual review and action. Does not modify any database records. [9]                                       |
| [`find-items-to-unrestrict-rack`](cronjobs/find-items-to-unrestrict-rack)   | Perl script  | Queries two Rackham dissertation collections (hardcoded UUIDs) for items whose `dc.date.issued` year equals the current year minus 95, and prints a list of their handles to STDOUT for manual review and action. Does not modify any database records. [¹0]                                        |
| [`meta_file_author`](cronjobs/meta_file_author)                             | XML template | Dublin Core XML template with `$HANDLE` and `$METAVAKYE` placeholders for batch-setting `dc.contributor.author` via `itemupdate`. [¹¹]                                                                                                                                                              |
| [`meta_file_delete`](cronjobs/meta_file_delete)                             | XML template | Dublin Core XML fragment template for targeting an item by handle in `itemupdate`; contains only a `$HANDLE` placeholder for `dc.identifier.uri`. (Copy of the top-level template, kept here for local use by cronjob scripts.)                                                                     |
| [`meta_file_desc`](cronjobs/meta_file_desc)                                 | XML template | Dublin Core XML snippet with `$HANDLE` and `$METAVAKYE` placeholders for batch-setting `dc.description` via `itemupdate`. [¹¹]                                                                                                                                                                      |
| [`perl_mailer`](cronjobs/perl_mailer)                                       | Perl script  | Reusable mailer utility. Accepts `-s <subject>` and `-e <recipient(s)>` on the command line (semicolon-separated for multiple recipients); reads message body from STDIN and sends it via the mail server configured in `mail__P__server`.                                                          |
| [`pubmedV2`](cronjobs/pubmedV2)                                             | Perl script  | Queries a specific DSpace collection (hardcoded UUID `4fdfaf57-…`) for items lacking PubMed IDs, calls the PubMed E-utilities API (via `NIH_API` and `NCBI_API` env vars) to look up IDs by DOI or title, and writes matching IDs back to `dc.relation` metadata.                                   |
| [`report-about-dearborn-items`](cronjobs/report-about-dearborn-items)       | Perl script  | Generates a tab-delimited report of non-embargoed, non-withdrawn items in the Dearborn collections (handle, selected metadata fields) and emails it via `mail__P__server`.                                                                                                                          |
| [`report-double-original`](cronjobs/report-double-original)                 | Perl script  | Queries `item2bundle` for items that have more than one ORIGINAL bundle (i.e., contain duplicate ORIGINAL bundles, not merely multiple bitstreams within one bundle) and prints those items for manual review. [¹²]                                                                                 |
| [`report-embargo`](cronjobs/report-embargo)                                 | Perl script  | Iterates over all rows in the `umrestricted` table; for each item that has at some point had its restrictions released (`EverBeenFree`), reports the current bitstream permission state. Highlights items that were embargoed after initial deposit. Does not display scheduled release dates. [¹³] |
| [`report-tombstone`](cronjobs/report-tombstone)                             | Perl script  | Finds items whose `dc.description.provenance` begins with `Removed from` (the tombstone provenance string) and reports their bitstream permission state; used to audit withdrawn/tombstoned items whose access-start date may need updating.                                                        |
| [`report-too-many-authors`](cronjobs/report-too-many-authors)               | Perl script  | Queries `metadatavalue` for items exceeding a hardcoded threshold of `dc.contributor.author` entries and emails a report to the configured address via `mail__P__server`.                                                                                                                           |
| [`report-too-many-db-connections`](cronjobs/report-too-many-db-connections) | Perl script  | Counts PostgreSQL connections "in transaction" for the DSpace database by piping `ps -ef` through `grep` to match "in trans" process listings, and emails an alert to a hardcoded address if the count exceeds 10.                                                                                  |
| [`update_author_list_too_long`](cronjobs/update_author_list_too_long)       | Perl script  | Fixes items whose author metadata list exceeds the display limit by truncating or restructuring the `dc.contributor.author` entries and staging corrected XML for `itemupdate`. Uses `BASE_DATA_DIR` and `BASE_BIN_DIR` env vars.                                                                   |

---

## `cronjobs/wrappers/` — Kubernetes CronJob Entrypoints

Bash wrappers that provide environment-aware execution, logging, and email
notifications for scheduled jobs. Job wrappers share `_runner.sh`; the argument
is the target environment.

| File                                                                                     | Type         | Description                                                                                                                                                                                  |
|------------------------------------------------------------------------------------------|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`_runner.sh`](cronjobs/wrappers/_runner.sh)                                              | Bash helper  | Shared wrapper runner. Validates the environment and job allowlist, captures output, sends configured notifications, and maps job results to exit statuses.                                  |
| [`checksum-checker.sh`](cronjobs/wrappers/checksum-checker.sh)                           | Bash wrapper | Runs the custom `cronjobs/check-checksum` query in anomaly-reporting mode; sends a notification and exits 10 when anomalous checksum-history result codes are found.                          |
| [`doi-process.sh`](cronjobs/wrappers/doi-process.sh)                                     | Bash wrapper | Runs the DSpace DOI organiser's update, reserve, register, and delete operations; adds quiet mode in staging and returns a failure if any operation fails. Queued metadata updates report individual failures and continue with remaining DOIs; successful changes are committed before a nonzero exit if any update failed. |
| [`dspace-checksum-checker.sh`](cronjobs/wrappers/dspace-checksum-checker.sh)             | Bash wrapper | Runs `dspace checker -d 1h -p` in staging; invokes DSpace's built-in bitstream checksum checker.                                                                                              |
| [`email-aptrust-bagging-errors.sh`](cronjobs/wrappers/email-aptrust-bagging-errors.sh)   | Bash wrapper | Runs the APTrust bagging-error and status checks in anomaly-reporting mode; sends a wrapper notification when issues are found.                                                              |
| [`filter-media.sh`](cronjobs/wrappers/filter-media.sh)                                   | Bash wrapper | Runs DSpace `filter-media` in staging and the custom `filter-media-cronjob` in production; treats reported per-item filtering errors as anomalies.                                          |
| [`index-discovery.sh`](cronjobs/wrappers/index-discovery.sh)                             | Bash wrapper | Runs `dspace index-discovery`; enabled in production, staging, demo, and workshop.                                                                                                            |
| [`index-oai.sh`](cronjobs/wrappers/index-oai.sh)                                         | Bash wrapper | Runs `dspace oai import`; enabled in production, staging, demo, and workshop.                                                                                                                 |
| [`monthly-report.sh`](cronjobs/wrappers/monthly-report.sh)                               | Bash wrapper | Runs `monthly_report` and emails its full output as a report, including when the command succeeds.                                                                                            |
| [`move-to-aptrust-regular.sh`](cronjobs/wrappers/move-to-aptrust-regular.sh)             | Bash wrapper | Runs the regular APTrust export pipeline; propagates failures from item processing and upload.                                                                                               |
| [`report-about-dearborn-items.sh`](cronjobs/wrappers/report-about-dearborn-items.sh)     | Bash wrapper | Runs the Dearborn items report, preserving its attachment delivery and original email subject.                                                                                                |
| [`report-aptrust-status.sh`](cronjobs/wrappers/report-aptrust-status.sh)                 | Bash wrapper | Runs the APTrust status check in anomaly-reporting mode; sends a wrapper notification when non-Success results require attention.                                                             |
| [`report-double-original.sh`](cronjobs/wrappers/report-double-original.sh)               | Bash wrapper | Runs the duplicate-ORIGINAL-bundle report in anomaly-reporting mode; sends a notification and exits 10 when matches are found.                                                               |
| [`report-too-many-authors.sh`](cronjobs/wrappers/report-too-many-authors.sh)             | Bash wrapper | Runs the excessive-author report in anomaly-reporting mode; sends a notification and exits 10 when matches are found.                                                                        |
| [`sub-monthly.sh`](cronjobs/wrappers/sub-monthly.sh)                                     | Bash wrapper | Sends monthly DSpace subscriptions with `dspace subscription-send -f M`.                                                                                                                     |
| [`sub-weekly.sh`](cronjobs/wrappers/sub-weekly.sh)                                       | Bash wrapper | Sends weekly DSpace subscriptions with `dspace subscription-send -f W`.                                                                                                                     |
| [`tests/wrappers.sh`](cronjobs/wrappers/tests/wrappers.sh)                               | Bash test     | Isolated wrapper tests covering environment restrictions, command invocation, output capture, notifications, and exit-status handling.                                                     |

---

## `monthlies/` — Monthly Maintenance

Scripts run on a monthly cadence: Wiley ingest pipelines, ORCID updates,
metadata type corrections, and user/collection reports.

| File                                                                     | Type                 | Description                                                                                                                                                                                                                                                                                           |
|--------------------------------------------------------------------------|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`JOURNAL_SUBJECTS`](monthlies/JOURNAL_SUBJECTS)                         | plain-text data file | Hand-maintained list mapping journal titles to subject categories (format: title, blank line, subject in `topic / sub-topic` form, then `====` separator). Used by monthly reporting scripts to classify Wiley items by subject area.                                                                 |
| [`change-type-martha`](monthlies/change-type-martha)                     | Perl script          | Batch metadata tool that reads a list of item IDs from a text file and updates `dc.type` for each item by generating `itemupdate`-compatible XML files in a staging directory. Uses `BASE_DATA_DIR` and `BASE_BIN_DIR` env vars.                                                                      |
| [`check-retiree.rb`](monthlies/check-retiree.rb)                         | Ruby script          | Queries the MCommunity identity service (OAuth2 via `MCurl`, `MCauthorization`, and `MCclient_id` env vars) for a list of users and reports their current affiliation status; used to identify retired or departed depositors.                                                                        |
| [`clear-bit-description`](monthlies/clear-bit-description)               | Perl script          | Connects to the DSpace database, selects bitstreams whose description (`metadata_field_id=26`) contains `Restricted to U`, and prints the handle and bitstream information for each so the description field can be cleared.                                                                          |
| [`find-authors-monthly`](monthlies/find-authors-monthly)                 | Perl script          | Monthly routine that truncates the `orcid_info` table, then re-populates it by scanning `metadatavalue` for ORCID-bearing author entries; used to keep the ORCID lookup table current before running `update-orcid-values-monthly`.                                                                   |
| [`find-bit-change-perpmission`](monthlies/find-bit-change-perpmission)   | Perl script          | Finds items with a specific metadata field value (field_id 88) and for each bitstream whose resource policy does not match the expected group UUID, prints the SQL `UPDATE resourcepolicy` statement to stdout for review and manual execution.                                                       |
| [`prepare-wiley`](monthlies/prepare-wiley)                               | Perl script          | Large multi-stage Wiley Open Access ingest pipeline. Derives a monthly batch identifier (e.g., `NewAfterEmbargoFixApr24`), reads Wiley article package data, maps metadata to DSpace Dublin Core (title, authors, subjects, date, DOI, publisher), and stages a SimpleArchiveFormat batch for import. |
| [`prepare-wiley_1`](monthlies/prepare-wiley_1)                           | Perl script          | Extended Wiley ingest variant; includes additional metadata mapping steps and `dc.identifier.doi` registration compared to `prepare-wiley`.                                                                                                                                                           |
| [`prepare-wiley_2`](monthlies/prepare-wiley_2)                           | Perl script          | Wiley ingest reporting variant; connects to PostgreSQL and produces a summary report of items already loaded from the current Wiley batch.                                                                                                                                                            |
| [`prepare-wiley_3`](monthlies/prepare-wiley_3)                           | Perl script          | Third Wiley ingest variant with updated metadata mapping and export logic; effectively a later-generation replacement for `prepare-wiley_1`.                                                                                                                                                          |
| [`prepare-wiley_4`](monthlies/prepare-wiley_4)                           | Perl script          | Reads `dublin_core.xml` files from a hardcoded path under `/mnt/prep/wiley/…/NewAfterEmbargoFix{month}-open/`, encodes each as UTF-8, and rewrites the file in place; uses a `prev_month_string()` helper to compute the current batch month label.                                                   |
| [`replace-funny-char-for-wiley`](monthlies/replace-funny-char-for-wiley) | Perl script          | Reads `dublin_core.xml` files from a hardcoded path (`/mnt/prep/wiley/…/NewAfterEmbargoFixOct25-open/`), decodes UTF-8, replaces the Unicode replacement character (`\uFFFD`, displayed as `\xEF\xBF\xBD`) with a hyphen, and overwrites each file in place.                                          |
| [`report-martha-types`](monthlies/report-martha-types)                   | Perl script          | Iterates over all archived items and retrieves their `dc.type` value (field_id 66); produces two tab-delimited reports — one listing items with no type and their owning collection, and one listing all items with their type and collection.                                                        |
| [`report-users-out`](monthlies/report-users-out)                         | Perl script          | Queries all rows from the `eperson` table and for each email address checks external systems to determine if the user account is still active; used to identify departed or deactivated depositors.                                                                                                   |
| [`update-orcid-values-monthly`](monthlies/update-orcid-values-monthly)   | Perl script          | Monthly script that clears and rebuilds two ORCID staging directories under `BASE_DATA_DIR`, queries `metadatavalue` for `dc.identifier.name-orcid` values, and writes `itemupdate`-ready XML files to update ORCID identifiers on affected items.                                                    |

---

## `rackham/` — Rackham Dissertations

Scripts specific to the Rackham Graduate School dissertation collection:
ingest preparation and reporting.

| File                                               | Type        | Description                                                                                                                                                                                                                                                                                                        |
|----------------------------------------------------|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`prepare-rackham`](rackham/prepare-rackham)       | Perl script | Transforms Rackham XML metadata files and associated PDFs from `$BASE_DATA_DIR/rackham/{batch}` into a DSpace SimpleArchiveFormat batch import structure under `$BASE_DATA_DIR/rackham/archive`. Handles free, UM-restricted, and fully-embargoed items; includes the Deep Blue license text in each item package. |
| [`report-for-rackham`](rackham/report-for-rackham) | Perl script | Connects to the DSpace database, queries the Rackham dissertations collection (hardcoded UUID `c5a42028-…`), and writes a tab-delimited report (`dissertation.txt`) of handle, collection name, authors, title, filenames, and embargo type/date to `$BASE_DATA_DIR/rackham/`.                                     |

---

## `stats_monthlies/` — Monthly Statistics Reports

Scripts and shared module for generating and emailing monthly usage-statistics
reports to collection administrators and individual depositors.

| File                                                                                                     | Type        | Description                                                                                                                                                                                                                                                                                                                       |
|----------------------------------------------------------------------------------------------------------|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`StatsUtils.pm`](stats_monthlies/StatsUtils.pm)                                                         | Perl module | Utility module providing SQL generation and result post-processing helpers for custom DSpace statistics reporting. Key functions: `GetCollname` (resolve collection name from UUID by querying `metadatavalue`), `GetSQLAddedSearch` (build date-bounded item-count SQL), and related helpers used by the monthly report scripts. |
| [`find-alicia-stats`](stats_monthlies/find-alicia-stats)                                                 | Perl script | Scans `BASE_LOG_DIR/<month>_<year>/` log files (using `grep`) to count bitstream downloads for a hardcoded list of specific PDF filenames belonging to item handle `2027.42/163715` (an "Alicia" COVID-19 caregiver workbook collection); prints download totals per file and overall.                                            |
| [`find-size-stats`](stats_monthlies/find-size-stats)                                                     | Perl script | Connects to the DSpace database, calculates the previous month's date, and queries for total bitstream storage sizes broken down by collection; outputs a size-by-collection report.                                                                                                                                              |
| [`monthly_admin_report`](stats_monthlies/monthly_admin_report)                                           | Perl script | Monthly stats emailer for collection administrators. Computes the previous month, retrieves admin–collection mappings from the database, queries download/access counts per collection from the custom stats tables, and emails each admin their collection's report via `mail__P__server`.                                       |
| [`monthly_admin_report_for_bentley`](stats_monthlies/monthly_admin_report_for_bentley)                   | Perl script | Variant of `monthly_admin_report` tailored for Bentley Historical Library collections; applies the same stats query pipeline but filters to Bentley-specific collections and hard-codes the Bentley recipient email address.                                                                                                      |
| [`monthly_individual_report`](stats_monthlies/monthly_individual_report)                                 | Perl script | Generates and emails per-user monthly download reports. Queries registered individual recipients from the database, fetches their item download counts from the custom stats tables for the previous month, and sends personalised emails via `mail__P__server`.                                                                  |
| [`monthly_individual_report_based_on_author`](stats_monthlies/monthly_individual_report_based_on_author) | Perl script | Variant of `monthly_individual_report` that looks up item handles by `dc.contributor.author` name (not by submitter/eperson identity); emails download totals for all matching items to the address associated with each author name.                                                                                             |

---

## Review Notes

Changes made during systematic file-by-file review (2026-04-22):

[¹] **`meta_file` and `meta_file_orcid` placeholder name** — The actual value
placeholder in both templates is `$METAVAKYE` (verbatim in the source), not
the descriptive `$METAVALUE` or `$ORCID` used in the initial draft. Corrected
to match the literal token used in the files.

[²] **`meta_file_orcid` field qualifier** — The file sets
`<dcvalue element="identifier" qualifier="name-orcid">`, making the full
Dublin Core field `dc.identifier.name-orcid`, not the unqualified
`dc.identifier` that the initial description implied.

[³] **`find_crawlers` arguments and behaviour** — The script requires both
`-d YYYY/MM` *and* `-l <logDir>` (the log directory was omitted from the
initial description). It stores *all* item- and bitstream-view IPs in
`crawlerip` (not just known crawlers); the "crawler" identification happens
later in `remove_ips`. It also resolves hostnames via `all_ips` or `nslookup`
and updates `crawlerip.site`.

[4] **`prep-logs` environment variables and role** — The initial description
named `STATS_LOG_DIR` as one of the env vars; the script actually reads
`BASE_LOG_DIR` and `BASE_BIN_DIR`. More importantly, `prep-logs` is the
*orchestrator* for the entire monthly stats pipeline: it invokes
`find_crawlers`, `remove_ips`, `additonal_stats_data`, `ip_stats_data`, and
`consolidate_ips` in sequence, and also purges log files/dirs older than
4 months from `/dspace/data/log`.

[5] **`update_all_ip_table` actual function** — The initial description said it
"updates the custom IP lookup table with computed monthly totals." In reality it
performs reverse DNS lookups (`nslookup`, 5-second timeout) for IPs that have
no hostname in `all_ips` for the previous month, and writes the resolved
hostname to `crawlerip.site`. No monthly totals are computed.

[6] **`move-to-aptrust-regular` upload mechanism** — The initial description
said items are uploaded "via their REST API." The script actually uses the AWS
S3 CLI (`aws s3 cp … s3://$APTRUST_BUCKET_NAME/…`), not a REST API call.

[7] **`report-aptrust-status` command-line options and email** — The `-t` and
`-o` options parsed from the command line are immediately overridden by
hardcoded assignments (`$doTest = 'prod'`; `$UploadOrStatus = 'nothing'`) and
therefore have no effect at runtime. The script also does not send any email;
the initial description's "emails a summary report" was incorrect.

[8] **`filter-media-cronjob` item selection** — The initial description said it
selects "5,000 archived items." It actually selects up to 5,000 archived items
that *specifically* have at least one PDF in their ORIGINAL bundle **and** lack
a TEXT bundle entirely, targeting only those that need text extraction.

[9] **`find-items-to-unrestrict-bio` mechanism** — The initial description said
it queries the `umrestricted` table and "lifts access restrictions by updating
resource policies." The script does neither: it queries the Bio Station
collection by `dc.date.issued` year (current year − 75) and *prints a list of
handles to STDOUT* for a human operator to act on. No records are modified.

[¹0] **`find-items-to-unrestrict-rack` mechanism** — Same issue as [9]: the
script queries two Rackham dissertation collections by `dc.date.issued` year
(current year − 95) and prints handles for manual action. It does not query
`umrestricted` or remove any restrictions.

[¹¹] **`meta_file_author` and `meta_file_desc` placeholder names** — Like the
top-level meta templates (see [¹]), both cronjobs templates use `$METAVAKYE`
as the value placeholder, not the descriptive `$AUTHOR` or `$DESC` used in the
initial draft.

[¹²] **`report-double-original` query semantics** — The SQL is
`SELECT item_id, count(*) FROM item2bundle WHERE … HAVING count(*) > 1`, which
finds items with *more than one ORIGINAL bundle*. The initial description said
"more than one bitstream in their ORIGINAL bundle," which is a different (and
less severe) condition. The report includes every matching item, one per line,
and prints only the heading when no items match. Previously, it overwrote each
result and printed only the final matching item.

Run the report regression tests with
`prove backend/test/cronjobs/report-double-original.t` (no database required).

[¹³] **`report-embargo` actual logic** — The initial description said the script
"produces a report listing each embargoed item's handle and scheduled release
date." The actual code iterates `umrestricted`, calls `EverBeenFree()` for each
item, and for those that have been free (meaning they were embargoed *after*
initial deposit), reports the current bitstream permission state. Release dates
are not displayed.
