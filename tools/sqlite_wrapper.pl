#!/usr/bin/perl
#
# Wrapper around sqlite3 & strace that prints the number of bytes read
# and number of read() system calls at the end.
#
# Useful for testing hypotheses around sqlite3 index selection.
#
# Author: bradfitz@google.com
#

use strict;

my $db = shift;
my $query = shift;

die "Usage: <foo.db> <sql_select_where>\n" unless $db =~ /\.db$/ && -e $db && $query;

my $SQLITE_BIN = "out/host/linux-x86/bin/sqlite3";
unless (-x $SQLITE_BIN) {
  $SQLITE_BIN = $ENV{"ANDROID_BUILD_TOP"} . "/" . $SQLITE_BIN;
  unless (-x $SQLITE_BIN) {
      $SQLITE_BIN = `which sqlite3`;
      chomp $SQLITE_BIN;
  }
}
die "No $SQLITE_BIN" unless -x $SQLITE_BIN;

my $TMP_FILE = "/tmp/sqlite3_wrapper-$$.trace";

my $rv = system("strace",
                "-s", 200,
                "-o", $TMP_FILE,
                $SQLITE_BIN,
                $db,
                $query);
if ($rv != 0) {
  die "sqlite3 failed";
}

open(my $fh, $TMP_FILE) or die "Where did $TMP_FILE go? Error opening: $!\n";
my $bytes = 0;
my $reads = 0;
my $fd = undef;
while (my $line = <$fh>) {
  chomp $line;
  if (!defined($fd) &&
      $line =~ /^open\(.*\Q$db\E.* = (\d+)\s*$/) {
    $fd = $1;
  }
  next unless defined($fd);
  if ($line =~ /^read\($fd, .+ = (\d+)/) {
    $reads++;
    $bytes += $1;
  }
}

die "Error parsing strace output file.\n" unless defined($fd);

print "Bytes: $bytes  (# reads = $reads)\n";

END {
  unlink $TMP_FILE if $TMP_FILE;
}
