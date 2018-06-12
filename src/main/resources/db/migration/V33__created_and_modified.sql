-- Adding created and modified timestamps to entities directly

ALTER TABLE coderegistry ADD COLUMN created TIMESTAMP without time zone NULL;
ALTER TABLE coderegistry ADD COLUMN modified TIMESTAMP without time zone NULL;

ALTER TABLE codescheme ADD COLUMN created TIMESTAMP without time zone NULL;
ALTER TABLE codescheme ADD COLUMN modified TIMESTAMP without time zone NULL;

ALTER TABLE code ADD COLUMN created TIMESTAMP without time zone NULL;
ALTER TABLE code ADD COLUMN modified TIMESTAMP without time zone NULL;

ALTER TABLE externalreference ADD COLUMN created TIMESTAMP without time zone NULL;
ALTER TABLE externalreference ADD COLUMN modified TIMESTAMP without time zone NULL;

ALTER TABLE propertytype ADD COLUMN created TIMESTAMP without time zone NULL;
ALTER TABLE propertytype ADD COLUMN modified TIMESTAMP without time zone NULL;

ALTER TABLE extensionscheme ADD COLUMN created TIMESTAMP without time zone NULL;
ALTER TABLE extensionscheme ADD COLUMN modified TIMESTAMP without time zone NULL;

ALTER TABLE extension ADD COLUMN created TIMESTAMP without time zone NULL;
ALTER TABLE extension ADD COLUMN modified TIMESTAMP without time zone NULL;
