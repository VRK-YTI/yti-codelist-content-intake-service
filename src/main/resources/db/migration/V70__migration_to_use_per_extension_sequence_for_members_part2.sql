CREATE OR REPLACE FUNCTION PopulateExistingMembersSequenceIdValues() RETURNS INTEGER AS $$
DECLARE
  rec RECORD;
  theUpdateSqlCommand varchar;
  theUuid varchar;
BEGIN
  FOR rec IN SELECT id, extension_id
             FROM member
             ORDER BY created
    LOOP
      RAISE NOTICE '%', rec.id;
      RAISE NOTICE '%', rec.extension_id;
      theUuid := rec.id::varchar;
      theUpdateSqlCommand := 'UPDATE member SET sequence_id = (SELECT nextval(''seq_for_ext_'
                               || replace(rec.extension_id::varchar, '-', '_')
                               || '''))'
                               || ' WHERE id = ''' || theUuid || '''';
      EXECUTE theUpdateSqlCommand;
    END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;

SELECT * INTO TEMP irrelevant FROM PopulateExistingMembersSequenceIdValues();

DROP FUNCTION PopulateExistingMembersSequenceIdValues();

COMMIT;