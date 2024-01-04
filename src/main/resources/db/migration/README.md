# YugabyteDB Migrations README

## Flyway Migrations

Flyway is designed to use at least two connections, one for the metadata table and one for the migrations. If the
migration flow modifies the system catalog, queries in the metadata session will fail with the catalog snapshot
exception because Flyway keeps the same transaction for the metadata connection. It needs to be retried.

Due to this it is highly recommended to customize the FlywayMigrationStrategy and wrap the migrate() operation with a
RetryTemplate (see the project's FlywayConfig).

This also means that the migration's DDL needs to be written in an idempotent fashion because DDL operations are not
transactional and cannot be rolled back by Flyway.  Essentially, wherever possible use "CREATE ... IF NOT EXISTS ..."
DDL.

## DDL Operations

### CREATE INDEX

When an index is created in YugabyteDB, the default uses an online safe backfill
process known as CONCURRENTLY (
see [create index semantics](https://docs.yugabyte.com/preview/api/ysql/the-sql-language/statements/ddl_create_index/#semantics)).
This operation has three distinct phases that each increment the
`pg_yb_catalog_version`.`current_version` by one.

If the table is empty or very small, these version changes can come in rapid succession
and may trigger online transaction latency due to the catalog metadata potentially being
refreshed by the client connection three distinct times.

In general, when creating a new index on an empty table consider using `CREATE INDEX
NONCONCURRENTLY ...`. Specifically when you know there will be no DML performed on the
table while the index itself is being created. This will result in only one catalog
version change when it is done.

Also refer to Franck Pachot's
blog: [CREATE INDEX in YugabyteDB: online or fast?](https://dev.to/yugabyte/create-index-in-yugabytedb-online-or-fast-2dl3)

#### Backfill

If the table already exists and could be written to during the creation phases, the
default (CONCURRENTLY) will generally work since the three phases will not complete
immediately due to the backfill itself taking some time. However, if concerned about
it being too fast, you can force a delay to the process using:

```postgresql
set yb_index_state_flags_update_delay to 10000; -- use a large enough delay for the app
```

The default is only 1000 (1s) which may be too short and cause observable latency spikes
in online transactions. This will cause index creation to become slower as this delay
effectively sleeps two times (although concerns about this should be limited as the
trade-off is potentially impacting online performance).

TODO research CREATE INDEX DEFFER?

### Catalog Breaking Changes

Specific DDL operations are considered so significant that they are considered catalog
breaking. These operations typically indicates something significant has structurally
changed so that even in-flight transactions need to be aborted and restarted. This can
have a negative impact if applied while online operations are ongoing.

This effect can be more pronounced in a universe with many separate database instances.
As of 2.18, catalog breaking version is tracked on a common table, so a breaking change
in one database instance is seen by all the others. Feature development for a
per-database catalog is ongoing and expected to be in tech preview by 2.20.2 (and
presumably feature release by 2.22).

Most ALTER commands are considered breaking except ALTER TABLE (except when also using
RENAME). DROP and REVOKE commands are also generally considered catalog breaking.

To determine for sure if a DDL command is breaking, test it out by first looking at the
`last_breaking_version` in the `pg_yb_catalog_version` table:

```postgresql
select *
from pg_yb_catalog_version;
```

Then run a test command and re-query the YugabyteDB catalog. If the `last_breaking_version`
changes, then the command is considered breaking and care should be used.

In cases where it is known that the effect of the command run is not disruptive to any active
connections, it is possible to preface the command with:

```postgresql
set yb_make_next_ddl_statement_nonbreaking = true;
```

This will cause the very next DDL statement executed in the session to be considered as
non-breaking (even if it is). This essentially suppresses the `last_breaking_version`
from being incremented. Caution should be used if such DDL command could cause
active connections to fail if the change would go unacknowledged.

Examples:

- Adding a column to an existing table with a constraint or default value.
  - Risks: in-flight transactions may violate constraint and/or miss default.
  - Remediation: table can be checked and manually patch any affected rows.
- Removing or renaming an existing table or column currently being used by the application.
