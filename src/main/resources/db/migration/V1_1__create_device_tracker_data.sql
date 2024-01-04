--
-- load in some sample data that can be used for testing
--

insert into yb_device_tracker (device_id, media_id, status, updated_date)
select uuid('cdd7cacd-8e0a-4372-8ceb-' || lpad(seq::text, 12, '0')),
       '48d1c2c2-0d83-43d9-' || lpad(seq2::text, 4, '0') || '-' || lpad(seq::text, 12, '0'),
       'ACTIVE' || seq,
       clock_timestamp()
from generate_series(0, ${deviceCount}) as seq,
     generate_series(0, ${mediaPerDeviceCount}) seq2;