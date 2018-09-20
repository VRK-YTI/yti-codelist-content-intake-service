-- Adding new member fields and changing existing one

ALTER TABLE member RENAME membervalue TO membervalue_1;
ALTER TABLE member ADD COLUMN membervalue_2 text NULL;
ALTER TABLE member ADD COLUMN membervalue_3 text NULL;