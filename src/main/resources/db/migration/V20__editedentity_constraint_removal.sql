-- Fully removing constraints on editedentity

ALTER TABLE editedentity DROP CONSTRAINT fk_commit_id;
ALTER TABLE editedentity DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE editedentity DROP CONSTRAINT fk_code_id;
ALTER TABLE editedentity DROP CONSTRAINT fk_externalreference_id;
ALTER TABLE editedentity DROP CONSTRAINT fk_extension_id;
ALTER TABLE editedentity DROP CONSTRAINT fk_extensionscheme_id;
