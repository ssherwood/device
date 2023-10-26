CREATE TABLE yb_device_tracker
(
    device_id    uuid        NOT NULL,
    media_id     text        NOT NULL,
    status       varchar(16) NOT NULL,
    created_date timestamptz NOT NULL default current_timestamp,
    updated_date timestamptz NOT NULL default current_timestamp,
    CONSTRAINT yb_device_tracker_pkey PRIMARY KEY (device_id HASH, media_id ASC)
) SPLIT INTO 100 TABLETS;

-- create index on yb_device_tracker (updated_date) split into 100 tablets;

create index on yb_device_tracker((yb_hash_code(updated_date)%16) asc, updated_date desc) split at values( (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13), (14) );


-- load
insert into yb_device_tracker (device_id, media_id, status, updated_date)
select uuid('cdd7cacd-8e0a-4372-8ceb-' || lpad(seq::text, 12, '0')),
       '48d1c2c2-0d83-43d9-' || lpad(seq2::text, 4, '0') || '-' || lpad(seq::text, 12, '0'),
       'ACTIVE' || seq, clock_timestamp()
from generate_series(0, 11000) as seq,
     generate_series(0, 60) seq2;



CREATE TABLE yb_device_tracker2
(
    device_id    uuid        NOT NULL,
    media_id     text        NOT NULL,
    account_id   uuid        NOT NULL,
    status       varchar(16) NOT NULL,
    created_date timestamptz NOT NULL default current_timestamp,
    updated_date timestamptz NOT NULL default current_timestamp,
    CONSTRAINT yb_device_tracker2_pkey PRIMARY KEY (device_id HASH, media_id ASC)
) SPLIT INTO 30 TABLETS;

insert into yb_device_tracker2 (device_id, media_id, account_id, status, updated_date)
select uuid('cdd7cacd-8e0a-4372-8ceb-' || lpad(seq::text, 12, '0')),
       '48d1c2c2-0d83-43d9-' || lpad(seq2::text, 4, '0') || '-' || lpad(seq::text, 12, '0'),
       uuid('ff710c59-1e6d-47f9-a775-' || lpad(seq::text, 12, '0')),
       'ACTIVE' || seq, clock_timestamp()
from generate_series(0, 500000) as seq,
     generate_series(0, 6) seq2;

create index on yb_device_tracker2(account_id) split into 30 tablets;

-- splits into 15 tablets
create index on yb_device_tracker2((yb_hash_code(updated_date)%16) asc, updated_date desc) split at values( (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13), (14) );
