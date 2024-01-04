--
-- Adding second device tracker table variant for testing
--

CREATE TABLE IF NOT EXISTS yb_device_tracker2
(
    device_id    uuid        NOT NULL,
    media_id     text        NOT NULL,
    account_id   uuid        NOT NULL,
    status       varchar(16) NOT NULL,
    created_date timestamptz NOT NULL default current_timestamp,
    updated_date timestamptz NOT NULL default current_timestamp,
    CONSTRAINT yb_device_tracker2_pkey PRIMARY KEY (device_id HASH, media_id ASC)
) SPLIT INTO 30 TABLETS;

CREATE INDEX ${ybIndexMode} IF NOT EXISTS yb_device_tracker2_account_id_idx ON yb_device_tracker2 (account_id) SPLIT INTO 30 TABLETS;
