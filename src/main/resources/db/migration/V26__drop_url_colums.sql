-- Drop unused url columns from db

ALTER TABLE coderegistry DROP COLUMN url;
ALTER TABLE codescheme DROP COLUMN url;
ALTER TABLE code DROP COLUMN url;

-- Renaming externalreference url to new name

ALTER TABLE externalreference RENAME COLUMN url TO referenceurl;
