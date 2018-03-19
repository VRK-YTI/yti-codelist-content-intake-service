-- Adding url references

ALTER TABLE coderegistry ADD url text NOT NULL DEFAULT '';
ALTER TABLE codescheme ADD url text NOT NULL DEFAULT '';
ALTER TABLE code ADD url text NOT NULL DEFAULT '';
