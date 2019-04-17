CREATE OR REPLACE FUNCTION FixMemberUris() RETURNS INTEGER AS $$
DECLARE
  rec RECORD;
  theUpdateSqlCommand varchar;
  oldUri varchar;
  newUri varchar;
  theUuid varchar;
BEGIN
  FOR rec IN SELECT id, uri, sequence_id
             FROM member
             ORDER BY created
    LOOP
      RAISE NOTICE '%', rec.id;
      RAISE NOTICE '%', rec.uri;
      RAISE NOTICE '%', rec.sequence_id;
      oldUri := rec.uri::varchar;
      newUri := replace(oldUri, rec.id::varchar, rec.sequence_id::varchar);
      theUuid := rec.id::varchar;
      theUpdateSqlCommand := 'UPDATE member SET uri = ''' || newUri || ''' WHERE id = ''' || theUuid || '''';
      EXECUTE theUpdateSqlCommand;
    END LOOP;
  RETURN 1;
END;
$$ LANGUAGE plpgsql;

SELECT * INTO TEMP irrelevantTempTable FROM FixMemberUris();

DROP FUNCTION FixMemberUris();

COMMIT;