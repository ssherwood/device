--
-- Assumptions:
-- The target database already exists.
-- Flyway will create the schema in the target database (see spring.flyway.default-schema)
--

CREATE TABLE IF NOT EXISTS yb_device_tracker
(
    device_id    uuid        NOT NULL,
    media_id     text        NOT NULL,
    status       varchar(16) NOT NULL,
    created_date timestamptz NOT NULL default current_timestamp,
    updated_date timestamptz NOT NULL default current_timestamp,
    CONSTRAINT yb_device_tracker_pkey PRIMARY KEY (device_id HASH, media_id ASC)
) SPLIT INTO 100 TABLETS;

-- makes use of a composite hash % mod + range index technique for dates
-- this can run be non-concurrently because the table was just created above
CREATE INDEX ${ybIndexMode} IF NOT EXISTS yb_device_tracker_hash_updated_date_idx ON yb_device_tracker ((yb_hash_code(updated_date) % 16) ASC, updated_date DESC) SPLIT AT VALUES( (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13), (14) );
