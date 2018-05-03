-- Rename editedentry and add id column

ALTER TABLE editedentry DROP CONSTRAINT editedentry_pkey;
ALTER TABLE editedentry ALTER COLUMN codescheme_id DROP NOT NULL;
ALTER TABLE editedentry ALTER COLUMN code_id DROP NOT NULL;
ALTER TABLE editedentry ALTER COLUMN codescheme_id DROP NOT NULL;
ALTER TABLE editedentry ALTER COLUMN extension_id DROP NOT NULL;
ALTER TABLE editedentry ALTER COLUMN extensionscheme_id DROP NOT NULL;
ALTER TABLE editedentry ALTER COLUMN externalreference_id DROP NOT NULL;
ALTER TABLE editedentry RENAME TO editedentity;
ALTER TABLE editedentity ADD id uuid NOT NULL;
ALTER TABLE editedentity ADD CONSTRAINT editedentity_pkey PRIMARY KEY (id);
ALTER TABLE editedentity DROP CONSTRAINT fk_commit_id;
ALTER TABLE editedentity ALTER COLUMN commit_id SET DATA TYPE text;
ALTER TABLE editedentity DROP CONSTRAINT editedentry_commit_id_key;

-- Refactor commit

ALTER TABLE commit DROP CONSTRAINT fk_useremail;
ALTER TABLE commit DROP COLUMN useremail;
ALTER TABLE commit ALTER COLUMN id SET DATA TYPE text;
ALTER TABLE commit ADD description text NULL;
ALTER TABLE commit ADD user_id uuid NULL;

-- Recreate constraint for commit in editedentity

ALTER TABLE editedentity ADD CONSTRAINT fk_commit_id FOREIGN KEY (commit_id) REFERENCES commit (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

-- Drop unnecessary tables

DROP TABLE user_organization;
DROP TABLE ytiuser;
