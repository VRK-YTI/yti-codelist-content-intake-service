-- Removing broader code fk from code

ALTER TABLE code DROP CONSTRAINT fk_broadercode_id;
