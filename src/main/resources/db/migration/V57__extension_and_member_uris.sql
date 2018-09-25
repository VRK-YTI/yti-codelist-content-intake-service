-- Adding uri columns to extension and member

ALTER TABLE extension ADD COLUMN uri text UNIQUE NULL;
ALTER TABLE member ADD COLUMN uri text UNIQUE NULL;