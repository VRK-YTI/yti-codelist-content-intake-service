--this stuff at the beginning is only needed to ensure no hickups during deployment in DEV environments due trash in the db.
ALTER TABLE member DROP IF EXISTS sequence_id;
ALTER TABLE member DROP CONSTRAINT IF EXISTS unique_sequence_order_per_extension;
ALTER TABLE member ADD sequence_id INTEGER;
ALTER TABLE member ADD CONSTRAINT unique_sequence_order_per_extension UNIQUE (extension_id, sequence_id);

CREATE OR REPLACE FUNCTION CreateSequencesForExistingExtensions() RETURNS INTEGER AS $$
DECLARE
  rec RECORD;
  theSequenceName varchar;
  theCreateSequenceSqlCommand varchar;
  theDropSequenceIfExistsSqlCommand varchar;
BEGIN
  FOR rec IN SELECT id
             FROM extension
             ORDER BY created
    LOOP
      RAISE NOTICE '%', rec.id;
      theSequenceName := 'seq_for_ext_' || replace(rec.id::varchar, '-', '_');
      theDropSequenceIfExistsSqlCommand := 'DROP SEQUENCE IF EXISTS ' || theSequenceName;
      theCreateSequenceSqlCommand := 'CREATE SEQUENCE ' || theSequenceName;
      EXECUTE theDropSequenceIfExistsSqlCommand;
      EXECUTE theCreateSequenceSqlCommand;
    END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;

SELECT * INTO TEMP irrelevant_return_value FROM CreateSequencesForExistingExtensions();

DROP FUNCTION CreateSequencesForExistingExtensions();

COMMIT;