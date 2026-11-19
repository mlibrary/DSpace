use strict;
use warnings;
use Test::More;
use File::Temp qw(tempdir);
use File::Path qw(make_path);
use FindBin;
use IPC::Open3;
use Symbol qw(gensym);

my $work = tempdir(CLEANUP => 1);
my $bin = "$FindBin::Bin/..";

sub fixture {
    my ($path, $content) = @_;
    $path = "$work/$path";
    $path =~ m{^(.*)/[^/]+$};
    make_path($1);
    open my $fh, '>', $path or die "$path: $!";
    print {$fh} $content or die "$path: $!";
    close $fh or die "$path: $!";
}

fixture('DBI.pm', <<'MOCK');
package DBI;
sub connect { bless {}, 'MockDB' }
package MockDB;
sub prepare { bless { sql => $_[1] }, 'MockStatement' }
sub disconnect { 1 }
sub errstr { 'mock DB failure' }
package MockStatement;
sub execute {
    return 0 if $ENV{TEST_SCENARIO} eq 'db-failure';
    my $sql = $_[0]{sql};
    my $bad = $ENV{TEST_SCENARIO} eq 'anomaly';
    my @rows;
    if ($sql =~ /select distinct result/) {
        @rows = (['CHECKSUM_MISMATCH']) if $bad;
    } elsif ($sql =~ /select item_id, count/) {
        @rows = (['item-uuid', 2]) if $bad;
    } elsif ($sql =~ /select dspace_object_id, count/) {
        @rows = (['item-uuid', 101]) if $bad || $ENV{TEST_SCENARIO} eq 'not-archived';
    } elsif ($sql =~ /select uuid from item where in_archive=true and uuid/) {
        @rows = (['item-uuid']) if $bad;
    } elsif ($sql =~ /select count\(\*\) from aptrust_bagging_error/) {
        @rows = ([$bad ? 2 : 0]);
    } elsif ($sql =~ /select object_identifier from aptrust_object_status/) {
        @rows = (['umich.edu/deepbluedocs.example']) if $bad || $ENV{TEST_SCENARIO} eq 'status-anomaly';
    } elsif ($sql =~ /select handle, bag, date_real/) {
        @rows = (['2027.42/example', 'bag.tar', '2026-01-01']);
    } elsif ($sql =~ /select handle from handle where resource_type_id/) {
        @rows = (['2027.42/example']);
    } elsif ($sql =~ /select resource_id from handle/) {
        @rows = (['item-uuid']);
    } elsif ($sql =~ /select text_value from metadatavalue where metadata_field_id = 64 and dspace_object_id = /) {
        @rows = (['A title']) unless $bad;
    } elsif ($sql =~ /select uuid from item where in_archive=true$/) {
        @rows = (['item-uuid']);
    } elsif ($sql =~ /select bitstream_id from bundle2bitstream/ && $sql =~ /ORIGINAL/) {
        @rows = (['bit-uuid']);
    } elsif ($sql =~ /select text_value from metadatavalue where metadata_field_id = 64 and dspace_object_id=/) {
        @rows = (['document.pdf']);
    } elsif ($sql =~ /select handle from handle where resource_id/) {
        @rows = (['2027.42/example']);
    }
    $_[0]{rows} = \@rows;
    return 1;
}
sub fetchrow_array { my $row = shift @{$_[0]{rows}}; return $row ? @$row : (); }
sub finish { 1 }
sub errstr { 'mock DB failure' }
1;
MOCK

fixture('Mail/Mailer.pm', <<'MOCK');
package Mail::Mailer;
use Symbol qw(gensym);
sub new { bless gensym(), 'Mail::Mailer' }
sub open {
    my ($self, $headers) = @_;
    return 0 if $ENV{TEST_SCENARIO} eq 'mail-open-failure';
    CORE::open($self, '>>', $ENV{TEST_MAIL_LOG}) or die $!;
    print {$self} "$headers->{Subject}\n";
    return $self;
}
sub close {
    CORE::close($_[0]) or die $!;
    return $ENV{TEST_SCENARIO} ne 'mail-close-failure';
}
1;
MOCK

fixture('MIME/Lite.pm', <<'MOCK');
package MIME::Lite;
sub new { my $class = shift; bless {@_}, $class }
sub attach { 1 }
sub send {
    my ($self) = @_;
    return 0 if $ENV{TEST_SCENARIO} eq 'mail-close-failure';
    open my $fh, '>>', $ENV{TEST_MAIL_LOG} or die $!;
    print {$fh} "$self->{Subject}\n";
    close $fh or die $!;
    return 1;
}
1;
MOCK

fixture('LWP/Simple.pm', "package LWP::Simple; sub import {} 1;\n");
fixture('Error.pm', "package Error; sub import {} 1;\n");
fixture('LWP/UserAgent.pm', <<'MOCK');
package LWP::UserAgent;
sub new { bless {}, __PACKAGE__ }
sub agent {}
sub timeout {}
sub get { bless {}, 'MockResponse' }
package MockResponse;
sub status_line { 'mock HTTP response' }
sub is_success { $ENV{TEST_SCENARIO} ne 'http-failure' }
sub content { '{}' }
1;
MOCK
fixture('JSON/XS.pm', <<'MOCK');
package JSON::XS;
sub import { no strict 'refs'; *{caller() . '::decode_json'} = \&decode_json; }
sub decode_json {
    my $status = $ENV{TEST_SCENARIO} eq 'anomaly' ? 'Failed' : 'Success';
    $status = undef if $ENV{TEST_SCENARIO} eq 'missing-status';
    return {results => [{action => 'Ingest', status => $status,
        storage_option => 'Standard', updated_at => '2026-01-01'}]};
}
1;
MOCK
fixture('bin/dspace', <<'MOCK');
#!/bin/bash
printf 'mock extraction output\n'
case "$TEST_SCENARIO" in
    filter-failure) exit 7 ;;
    anomaly) printf 'ERROR filtering mock item\n' ;;
esac
MOCK
chmod 0755, "$work/bin/dspace" or die $!;

sub run_script {
    my ($script, $scenario, $wrapped, @args) = @_;
    local %ENV = (%ENV, TEST_SCENARIO => $scenario, TEST_MAIL_LOG => "$work/deliveries.log",
        CRON_WRAPPER => $wrapped, CRON_ENVIRONMENT => 'production',
        DSPACE_BIN_DIR => "$work/bin", DSPACE_CLI_LOG => "$work/no-cli-log",
        BASE_BIN_DIR => "$work/bin", BASE_DATA_DIR => $work);
    unlink "$work/deliveries.log";
    my $err = gensym();
    my $pid = open3(my $in, my $out, $err, $^X, "-I$work", "$bin/$script", @args);
    close $in;
    my $output = do { local $/; (<$out> // '') . (<$err> // '') };
    waitpid($pid, 0);
    my $status = $? >> 8;
    my $mailed = -e "$work/deliveries.log" ? 1 : 0;
    diag("$script: $output") if $status != 0 && $status != 10 && $scenario !~ /failure|anomaly/;
    return ($status, $output, $mailed);
}

for my $script (qw(cronjobs/check-checksum cronjobs/report-double-original
        cronjobs/report-too-many-authors aptrust/email-aptrust-bagging-errors)) {
    my ($status, $out, $mailed) = run_script($script, 'success', 1);
    is($status, 0, "$script: no anomalies succeeds");
    ok(!$mailed, "$script: no success email");
    ($status, $out, $mailed) = run_script($script, 'anomaly', 1);
    is($status, 10, "$script: anomalies return 10");
    ok(!$mailed, "$script: wrapper owns anomaly email");
    like($out, qr/CHECKSUM_MISMATCH|ORIGINAL bundles = 2|count = 101|bagging error/,
        "$script: findings are printed");
    ($status) = run_script($script, 'db-failure', 1);
    isnt($status, 0, "$script: database errors fail");
    ($status, $out, $mailed) = run_script($script, 'anomaly', 0);
    is($status, 0, "$script: legacy exit status preserved");
    if ($script =~ /authors|bagging-errors/) {
        ok($mailed, "$script: legacy internal mail preserved");
    }
}

my ($status, $out, $mailed) = run_script('cronjobs/report-too-many-authors', 'not-archived', 1);
is($status, 0, 'unarchived authors are not an anomaly');
ok(!$mailed, 'unarchived authors do not email');

($status, $out) = run_script('aptrust/email-aptrust-bagging-errors', 'status-anomaly', 1);
is($status, 10, 'APTrust object errors alone are detected');
like($out, qr/umich.edu\/deepbluedocs.example/, 'object error identifiers are printed');

for my $scenario (qw(success anomaly http-failure missing-status)) {
    ($status, $out) = run_script('aptrust/report-aptrust-status', $scenario, 1);
    is($status, $scenario eq 'success' ? 0 : 10, "APTrust status: $scenario");
}

for my $scenario (qw(success anomaly filter-failure)) {
    ($status, $out) = run_script('cronjobs/filter-media-cronjob', $scenario, 1);
    is($status, $scenario eq 'success' ? 0 : 1, "media filter: $scenario");
}

# Dry run is essential: the real mover otherwise invokes destructive cleanup.
for my $scenario (qw(success anomaly)) {
    ($status, $out) = run_script('aptrust/move-to-aptrust-regular', $scenario, 1, '-d');
    is($status, $scenario eq 'success' ? 0 : 1, "APTrust dry run: $scenario");
}

for my $scenario (qw(success mail-open-failure mail-close-failure)) {
    ($status, $out, $mailed) = run_script('cronjobs/perl_mailer', $scenario, 1,
        '-s', 'production: test FAILED', '-e', 'test@example.invalid');
    if ($scenario eq 'success') {
        is($status, 0, 'mailer succeeds with working transport');
        ok($mailed, 'mailer sends message');
    } else {
        isnt($status, 0, "mailer exposes $scenario");
    }
}

for my $scenario (qw(success mail-close-failure)) {
    ($status, $out, $mailed) = run_script('cronjobs/report-about-dearborn-items', $scenario, 1);
    if ($scenario eq 'success') {
        is($status, 0, 'Dearborn report succeeds');
        ok($mailed, 'Dearborn attachment delivery is preserved');
        open my $fh, '<', "$work/deliveries.log" or die $!;
        like(scalar <$fh>, qr/^production: Dearborn/, 'Dearborn subject identifies environment');
        close $fh or die $!;
    } else {
        isnt($status, 0, 'Dearborn report exposes delivery failure');
    }
}

done_testing();
