ALTER TABLE yb_device_tracker2 add column description text;

UPDATE yb_device_tracker2 set description = 'DEFAULT';