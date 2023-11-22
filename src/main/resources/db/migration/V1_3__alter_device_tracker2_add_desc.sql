--
-- Simple modification to an existing table by adding a description
--
-- Note: this apply will assume that the table is active and being used
-- during the ALTER.  Applying a DEFAULT here could be catalog breaking
-- so instead we are following it with an UPDATE.
--
-- However, this is a mass updated that could have a slight impact on
-- in-flight transactions attempting to mutate rows in the table at
-- the time this migration is run.  Assumption is that with the
-- configured Spring Retry/backoff loop will prevent any errors at the
-- cost of some additional latency.
--

ALTER TABLE yb_device_tracker2 add column description text;

UPDATE yb_device_tracker2 set description = 'DEFAULT';