-- Migrate earlier member membervalue fields and install uuid-ossp extension

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO membervalue (id, member_id, value, created, modified, valuetype_id)
SELECT * FROM (SELECT uuid_generate_v4(), id, membervalue_1 FROM member as m WHERE membervalue_1 IS NOT NULL) a,
              (SELECT cast('2018-01-01' as timestamp without time zone), cast('2018-01-01' as timestamp without time zone)) b,
              (SELECT id FROM valuetype as vt WHERE localname = 'unaryOperator') c;

INSERT INTO membervalue (id, member_id, value, created, modified, valuetype_id)
SELECT * FROM (SELECT uuid_generate_v4(), id, membervalue_2 FROM member as m WHERE membervalue_2 IS NOT NULL) a,
              (SELECT cast('2018-01-01' as timestamp without time zone), cast('2018-01-01' as timestamp without time zone)) b,
              (SELECT id FROM valuetype as vt WHERE localname = 'comparisonOperator') c;

ALTER TABLE member DROP COLUMN membervalue_1;
ALTER TABLE member DROP COLUMN membervalue_2;
ALTER TABLE member DROP COLUMN membervalue_3;
