-- Drop modified timestamps from entities

ALTER TABLE coderegistry DROP modified;
ALTER TABLE codescheme DROP modified;
ALTER TABLE code DROP modified;
ALTER TABLE externalreference DROP modified;
