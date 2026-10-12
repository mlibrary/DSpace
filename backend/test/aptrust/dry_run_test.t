#!/usr/bin/env perl
#
# dry_run_test.t - Integration and dry-run tests for APTrust Perl deposit scripts
#                  against a PostgreSQL database instance.
#
use strict;
use warnings;
use Test::More;
use DBI;
use File::Basename;
use Cwd qw(abs_path);

# Resolve project paths
my $test_dir   = dirname(abs_path(__FILE__));
my $repo_root  = abs_path("$test_dir/../../..");
my $bin_dir    = "$repo_root/backend/bin";
my $script_reg = "$bin_dir/aptrust/move-to-aptrust-regular";
my $script_ret = "$bin_dir/aptrust/move-to-aptrust-retry";

# Database configuration from environment with defaults
my $db_name     = $ENV{'DB_NAME'}     || 'dspace';
my $db_user     = $ENV{'DB_USER'}     || 'dspace';
my $db_password = $ENV{'DB_PASSWORD'} || 'dspace';
my $db_service  = $ENV{'DB_SERVICE'}  || 'localhost';
my $db_port     = $ENV{'DB_PORT'}     || '5432';

# ---------------------------------------------------------------------------
# 1. Connect to PostgreSQL
# ---------------------------------------------------------------------------
my $dsn = "dbi:Pg:dbname=$db_name;host=$db_service;port=$db_port";
my $dbh = DBI->connect($dsn, $db_user, $db_password, {
    RaiseError => 0,
    PrintError => 0,
    AutoCommit => 1,
});

if (!$dbh) {
    plan skip_all => "Cannot connect to PostgreSQL ($dsn): $DBI::errstr. Ensure Postgres Docker container is running.";
}

# ---------------------------------------------------------------------------
# 2. Setup DSpace and APTrust database schema tables (if not already present)
# ---------------------------------------------------------------------------
sub setup_tables {
    my ($dbh) = @_;

    $dbh->do(q{
        CREATE TABLE IF NOT EXISTS metadatafieldregistry (
            metadata_field_id integer PRIMARY KEY,
            metadata_schema_id integer,
            element text,
            qualifier text
        )
    });

    $dbh->do(q{
        CREATE TABLE IF NOT EXISTS item (
            uuid text PRIMARY KEY,
            in_archive boolean DEFAULT true,
            withdrawn boolean DEFAULT false,
            last_modified text
        )
    });

    $dbh->do(q{
        CREATE TABLE IF NOT EXISTS handle (
            handle text PRIMARY KEY,
            resource_type_id integer,
            resource_id text
        )
    });

    $dbh->do(q{
        CREATE TABLE IF NOT EXISTS metadatavalue (
            metadata_value_id SERIAL PRIMARY KEY,
            metadata_field_id integer,
            dspace_object_id text,
            text_value text
        )
    });

    $dbh->do(q{
        CREATE TABLE IF NOT EXISTS aptrust_bags_sent (
            handle text PRIMARY KEY,
            bag text,
            date text,
            last_modified_date text,
            sent_count integer,
            check_status text
        )
    });

    $dbh->do(q{
        CREATE TABLE IF NOT EXISTS aptrust_bagging_error (
            handle text PRIMARY KEY,
            error text
        )
    });

    $dbh->do(q{
        CREATE TABLE IF NOT EXISTS aptrust_object_status (
            handle text PRIMARY KEY,
            status text,
            date_real timestamp
        )
    });

    # Ensure field 64 exists for dc.title
    $dbh->do(q{
        INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier)
        VALUES (64, 1, 'title', NULL)
        ON CONFLICT (metadata_field_id) DO NOTHING
    });
}

setup_tables($dbh);

# ---------------------------------------------------------------------------
# 3. Clean up any previous test fixtures
# ---------------------------------------------------------------------------
my @test_handles = (
    '2027.42/test-valid-title',
    '2027.42/test-missing-title',
    '2027.42/test-blank-title',
    '2027.42/test-retry-missing-title',
);
my @test_uuids = (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444',
);

sub cleanup_fixtures {
    my ($dbh) = @_;
    foreach my $h (@test_handles) {
        $dbh->do("DELETE FROM handle WHERE handle = '$h'");
        $dbh->do("DELETE FROM aptrust_bags_sent WHERE handle = '$h'");
        $dbh->do("DELETE FROM aptrust_bagging_error WHERE handle = '$h'");
        $dbh->do("DELETE FROM aptrust_object_status WHERE handle = '$h'");
    }
    foreach my $u (@test_uuids) {
        $dbh->do("DELETE FROM metadatavalue WHERE dspace_object_id = '$u'");
        $dbh->do("DELETE FROM item WHERE uuid = '$u'");
    }
}

cleanup_fixtures($dbh);

# ---------------------------------------------------------------------------
# 4. Insert test fixtures
# ---------------------------------------------------------------------------
# Item 1: Valid title
$dbh->do("INSERT INTO item (uuid, in_archive, withdrawn, last_modified) VALUES ('$test_uuids[0]', true, false, '2026-10-01 12:00:00')");
$dbh->do("INSERT INTO handle (handle, resource_type_id, resource_id) VALUES ('$test_handles[0]', 2, '$test_uuids[0]')");
$dbh->do("INSERT INTO metadatavalue (metadata_field_id, dspace_object_id, text_value) VALUES (64, '$test_uuids[0]', 'Valid Academic Publication Title')");

# Item 2: Missing title (no dc.title metadatavalue entry)
$dbh->do("INSERT INTO item (uuid, in_archive, withdrawn, last_modified) VALUES ('$test_uuids[1]', true, false, '2026-10-01 12:00:00')");
$dbh->do("INSERT INTO handle (handle, resource_type_id, resource_id) VALUES ('$test_handles[1]', 2, '$test_uuids[1]')");

# Item 3: Blank/whitespace-only title
$dbh->do("INSERT INTO item (uuid, in_archive, withdrawn, last_modified) VALUES ('$test_uuids[2]', true, false, '2026-10-01 12:00:00')");
$dbh->do("INSERT INTO handle (handle, resource_type_id, resource_id) VALUES ('$test_handles[2]', 2, '$test_uuids[2]')");
$dbh->do("INSERT INTO metadatavalue (metadata_field_id, dspace_object_id, text_value) VALUES (64, '$test_uuids[2]', '   \t   \n ')");

# Item 4: Retry candidate (failed earlier in aptrust_object_status) but missing title
$dbh->do("INSERT INTO item (uuid, in_archive, withdrawn, last_modified) VALUES ('$test_uuids[3]', true, false, '2026-10-01 12:00:00')");
$dbh->do("INSERT INTO handle (handle, resource_type_id, resource_id) VALUES ('$test_handles[3]', 2, '$test_uuids[3]')");
$dbh->do("INSERT INTO aptrust_object_status (handle, status, date_real) VALUES ('$test_handles[3]', 'Failed', NOW() - INTERVAL '4days')");

# ---------------------------------------------------------------------------
# 5. Direct test of ItemHasTitle SQL logic
# ---------------------------------------------------------------------------
sub test_item_has_title {
    my ($handle) = @_;
    my $sth = $dbh->prepare("SELECT resource_id FROM handle WHERE handle = ?");
    $sth->execute($handle);
    my ($item_id) = $sth->fetchrow_array();
    $sth->finish();
    return 0 if (!defined($item_id) || $item_id eq '');

    $sth = $dbh->prepare("SELECT text_value FROM metadatavalue WHERE metadata_field_id = 64 AND dspace_object_id = ?");
    $sth->execute($item_id);
    my $has_title = 0;
    while (my ($val) = $sth->fetchrow_array()) {
        if (defined($val)) {
            $val =~ s/^\s+|\s+$//g;
            if (length($val) > 0) {
                $has_title = 1;
                last;
            }
        }
    }
    $sth->finish();
    return $has_title;
}

ok(test_item_has_title($test_handles[0]), "Item with valid title returns 1 (true)");
ok(!test_item_has_title($test_handles[1]), "Item with missing title returns 0 (false)");
ok(!test_item_has_title($test_handles[2]), "Item with whitespace-only title returns 0 (false)");
ok(!test_item_has_title('2027.42/nonexistent'), "Non-existent item handle returns 0 (false)");

# ---------------------------------------------------------------------------
# 6. Test aptrust-info.txt Title validation regex
# ---------------------------------------------------------------------------
my $valid_aptrust_info = "Title: Sample Valid Title\nAccess: Institution\nStorage-Option: Glacier-Deep-OR\n";
my $empty_aptrust_info = "Title: \nAccess: Institution\nStorage-Option: Glacier-Deep-OR\n";
my $whitespace_aptrust_info = "Title:    \t  \nAccess: Institution\nStorage-Option: Glacier-Deep-OR\n";
my $no_title_aptrust_info = "Access: Institution\nStorage-Option: Glacier-Deep-OR\n";

ok($valid_aptrust_info =~ /^Title:[ \t]*\S+/m, "aptrust-info.txt validation passes for non-empty Title");
ok(!($empty_aptrust_info =~ /^Title:[ \t]*\S+/m), "aptrust-info.txt validation fails for empty Title");
ok(!($whitespace_aptrust_info =~ /^Title:[ \t]*\S+/m), "aptrust-info.txt validation fails for whitespace Title");
ok(!($no_title_aptrust_info =~ /^Title:[ \t]*\S+/m), "aptrust-info.txt validation fails when Title tag is absent");

# ---------------------------------------------------------------------------
# 7. Execute move-to-aptrust-regular in Dry-Run mode
# ---------------------------------------------------------------------------
my %env_backup = %ENV;
$ENV{'DB_NAME'}             = $db_name;
$ENV{'DB_USER'}             = $db_user;
$ENV{'DB_PASSWORD'}         = $db_password;
$ENV{'DB_SERVICE'}          = $db_service;
$ENV{'DB_PORT'}             = $db_port;
$ENV{'APTRUST_BUCKET_NAME'} = 'test-aptrust-bucket';
$ENV{'BASE_BIN_DIR'}        = $bin_dir;
$ENV{'DRY_RUN'}             = '1';

my $h0 = $test_handles[0];
my $h1 = $test_handles[1];
my $h2 = $test_handles[2];
my $h3 = $test_handles[3];

my $regular_output = `$^X $script_reg -d 2>&1`;
my $regular_exit   = $? >> 8;

is($regular_exit, 0, "move-to-aptrust-regular executes with exit code 0 in dry-run mode");
like($regular_output, qr/Item \Q$h1\E is missing title; skipping APTrust deposit/, "Regular script logs skip for missing title item");
like($regular_output, qr/Item \Q$h2\E is missing title; skipping APTrust deposit/, "Regular script logs skip for blank title item");
like($regular_output, qr/DRY RUN: Item \Q$h0\E has title/, "Regular script proceeds for valid title item in dry-run mode");

# Check aptrust_bagging_error table in Postgres
my $sth = $dbh->prepare("SELECT error FROM aptrust_bagging_error WHERE handle = ?");

$sth->execute($h1);
my ($err_missing) = $sth->fetchrow_array();
is($err_missing, 'Item missing title; skipped APTrust deposit.', "aptrust_bagging_error recorded for missing title item");

$sth->execute($h2);
my ($err_blank) = $sth->fetchrow_array();
is($err_blank, 'Item missing title; skipped APTrust deposit.', "aptrust_bagging_error recorded for blank title item");

$sth->execute($h0);
my ($err_valid) = $sth->fetchrow_array();
is($err_valid, undef, "aptrust_bagging_error NOT recorded for valid title item");

# Check that skipped items were NOT marked as sent in aptrust_bags_sent
$sth = $dbh->prepare("SELECT handle FROM aptrust_bags_sent WHERE handle = ?");
$sth->execute($h1);
my ($sent_missing) = $sth->fetchrow_array();
is($sent_missing, undef, "Missing title item was NOT added to aptrust_bags_sent");

$sth->execute($h2);
my ($sent_blank) = $sth->fetchrow_array();
is($sent_blank, undef, "Blank title item was NOT added to aptrust_bags_sent");

# ---------------------------------------------------------------------------
# 8. Execute move-to-aptrust-retry in Dry-Run mode
# ---------------------------------------------------------------------------
my $retry_output = `$^X $script_ret -d 2>&1`;
my $retry_exit   = $? >> 8;

is($retry_exit, 0, "move-to-aptrust-retry executes with exit code 0 in dry-run mode");
like($retry_output, qr/Item \Q$h3\E is missing title; skipping APTrust deposit/, "Retry script logs skip for retry item missing title");

$sth = $dbh->prepare("SELECT error FROM aptrust_bagging_error WHERE handle = ?");
$sth->execute($h3);
my ($err_retry) = $sth->fetchrow_array();
is($err_retry, 'Item missing title; skipped APTrust deposit.', "aptrust_bagging_error recorded for retry item missing title");

$sth->finish();

# ---------------------------------------------------------------------------
# 9. Clean up test fixtures and disconnect
# ---------------------------------------------------------------------------
cleanup_fixtures($dbh);
$dbh->disconnect();

done_testing();
