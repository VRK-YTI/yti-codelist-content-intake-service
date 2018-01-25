-- Adding duplicate value unique constraints to coderegistry codevalue

ALTER TABLE coderegistry ADD UNIQUE (codevalue);
