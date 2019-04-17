CREATE OR REPLACE FUNCTION make_seq_for_extension() RETURNS TRIGGER AS $$
DECLARE
  sql varchar := 'CREATE SEQUENCE seq_for_ext_' || replace(NEW.id::varchar, '-', '_');
BEGIN
  EXECUTE sql;
  return NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_creation_of_new_sequence_for_extension
  AFTER INSERT
  ON extension
  FOR EACH ROW
EXECUTE PROCEDURE make_seq_for_extension();

CREATE OR REPLACE FUNCTION drop_seq_for_extension() RETURNS TRIGGER AS $$
DECLARE
  sql varchar := 'DROP SEQUENCE IF EXISTS seq_for_ext_' || replace(OLD.id::varchar, '-', '_');
BEGIN
  EXECUTE sql;
  return OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_dropping_of_existing_sequence_upon_extension_deletion
  AFTER DELETE
  ON extension
  FOR EACH ROW
EXECUTE PROCEDURE drop_seq_for_extension();

ALTER TABLE member ADD sequence_id INTEGER;

ALTER TABLE member ADD CONSTRAINT unique_sequence_order_per_extension UNIQUE (extension_id, sequence_id);

COMMIT;