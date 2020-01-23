-- Reset all extension / member sequences to current max values
CREATE OR REPLACE FUNCTION resetExtensionSequenceCurrentValues() RETURNS INTEGER AS $$
DECLARE
  record RECORD;
  sequenceName varchar;
  maxValueCommand varchar;
  setSequenceValueCommand varchar;
  maxValue integer;
BEGIN
  FOR record IN SELECT id
             FROM extension
             ORDER BY created
    LOOP
      sequenceName := 'seq_for_ext_' || replace(record.id::varchar, '-', '_');
      maxValueCommand := 'SELECT max(sequence_id) FROM member WHERE extension_id = ''' || record.id || '''';
      EXECUTE maxValueCommand INTO maxValue;
      IF (maxValue > 0) THEN
        setSequenceValueCommand := 'SELECT SETVAL(''' || sequenceName || ''', ' || maxValue || ')';
        EXECUTE setSequenceValueCommand;
      END IF;
    END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;

SELECT * FROM resetExtensionSequenceCurrentValues();

DROP FUNCTION resetExtensionSequenceCurrentValues();

COMMIT;
