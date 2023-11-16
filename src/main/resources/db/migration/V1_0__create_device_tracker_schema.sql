CREATE TABLE yb_device_tracker
(
    device_id    uuid        NOT NULL,
    media_id     text        NOT NULL,
    status       varchar(16) NOT NULL,
    created_date timestamptz NOT NULL default current_timestamp,
    updated_date timestamptz NOT NULL default current_timestamp,
    CONSTRAINT yb_device_tracker_pkey PRIMARY KEY (device_id HASH, media_id ASC)
) SPLIT INTO 100 TABLETS;

create index on yb_device_tracker ((yb_hash_code(updated_date) % 16) asc, updated_date desc) split at values( (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13), (14) );

insert into yb_device_tracker (device_id, media_id, status, updated_date)
select uuid('cdd7cacd-8e0a-4372-8ceb-' || lpad(seq::text, 12, '0')),
       '48d1c2c2-0d83-43d9-' || lpad(seq2::text, 4, '0') || '-' || lpad(seq::text, 12, '0'),
       'ACTIVE' || seq,
       clock_timestamp()
from generate_series(0, 1100) as seq,
     generate_series(0, 60) seq2;