use strict;
use warnings;
use Test::More;
use File::Temp qw(tempdir);
use FindBin;

my $lib = tempdir(CLEANUP => 1);
open my $mock, '>', "$lib/DBI.pm" or die "Cannot create mock DBI: $!";
print {$mock} <<'MOCK';
package DBI;
use strict;
use warnings;
sub connect { return bless {}, 'DBI::Database' }

package DBI::Database;
sub prepare {
    my ($self, $sql) = @_;
    if ($sql =~ /select handle from handle where resource_id = '(\d+)'/) {
        return bless { rows => [["2027.42/$1"]] }, 'DBI::Statement';
    }
    return bless {
        rows => [map { [$_, 2] } 1 .. $ENV{REPORT_TEST_ITEMS}]
    }, 'DBI::Statement';
}
sub disconnect { return 1 }

package DBI::Statement;
sub execute { return 1 }
sub finish { return 1 }
sub fetchrow_array {
    my $row = shift @{$_[0]->{rows}};
    return $row ? @$row : ();
}
1;
MOCK
close $mock or die "Cannot close mock DBI: $!";

my $script = "$FindBin::Bin/../../bin/cronjobs/report-double-original";
my $heading = "Report of items that have more than one ORIGINAL bundle listed below (if any): \n";

for my $count (0, 1, 3) {
    local $ENV{REPORT_TEST_ITEMS} = $count;
    open my $report, '-|', $^X, "-I$lib", $script
        or die "Cannot run report: $!";
    my $output = do { local $/; <$report> };
    close $report;
    is($?, 0, "report exits successfully for $count matching items");
    my $expected = $heading . join('', map {
        "2027.42/$_ has these many ORIGINAL bundles = 2\n"
    } 1 .. $count);
    is($output, $expected, "report prints all $count matching items");
}

done_testing();
