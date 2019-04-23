CREATE OR REPLACE FUNCTION FixMemberEmptySequenceIds() RETURNS INTEGER AS $$
DECLARE
  rec RECORD;
  theUpdateSqlCommand varchar;
  theUuid varchar;
BEGIN
  FOR rec IN SELECT id, extension_id
             FROM member
             WHERE sequence_id IS NULL
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

SELECT * INTO TEMP irrevTemp FROM FixMemberEmptySequenceIds();

DROP FUNCTION FixMemberEmptySequenceIds();

COMMIT;