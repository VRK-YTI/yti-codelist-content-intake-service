-- Modifying extension and member uris to be NOT NULL type

UPDATE member SET uri = CONCAT((SELECT uri FROM extension WHERE id = member.extension_id), '/member/', member.id) WHERE uri IS NULL;

ALTER TABLE extension ALTER COLUMN uri SET NOT NULL;
ALTER TABLE member ALTER COLUMN uri SET NOT NULL;
