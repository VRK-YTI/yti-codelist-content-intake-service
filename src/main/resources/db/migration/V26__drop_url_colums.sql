-- Drop unused url columns from db

ALTER TABLE coderegistry DROP COLUMN url;
ALTER TABLE codescheme DROP COLUMN url;
ALTER TABLE code DROP COLUMN url;
ALTER TABLE externalreference DROP COLUMN url;
