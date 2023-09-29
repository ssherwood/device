CREATE TABLE yb_device_tracker
(
    device_id    uuid        NOT NULL,
    media_id     text        NOT NULL,
    status       varchar(16) NOT NULL,
    created_date timestamptz NOT NULL default current_timestamp,
    updated_date timestamptz NOT NULL default current_timestamp,
    CONSTRAINT yb_device_tracker_pkey PRIMARY KEY (device_id HASH, media_id ASC)
) SPLIT INTO 100 TABLETS;

create index on yb_device_tracker (updated_date) split into 100 tablets;

-- load
insert into yb_device_tracker (device_id, media_id, status)
select uuid('cdd7cacd-8e0a-4372-8ceb-' || lpad(seq::text, 12, '0')),
       '48d1c2c2-0d83-43d9-' || lpad(seq2::text, 4, '0') || '-' || lpad(seq::text, 12, '0'),
       'ACTIVE' || seq
from generate_series(0, 11000) as seq,
     generate_series(0, 60) seq2;