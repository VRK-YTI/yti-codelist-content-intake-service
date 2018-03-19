-- Adding proper CASCADE deletion rules

-- CodeRegistry

ALTER TABLE coderegistry_preflabel DROP CONSTRAINT fk_coderegistry_preflabel;
ALTER TABLE coderegistry_preflabel ADD CONSTRAINT fk_coderegistry_id FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE coderegistry_definition DROP CONSTRAINT fk_coderegistry_definition;
ALTER TABLE coderegistry_definition ADD CONSTRAINT fk_coderegistry_id FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

-- CodeScheme

ALTER TABLE codescheme_preflabel DROP CONSTRAINT fk_codescheme_preflabel;
ALTER TABLE codescheme_preflabel ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE codescheme_definition DROP CONSTRAINT fk_codescheme_definition;
ALTER TABLE codescheme_definition ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE codescheme_description DROP CONSTRAINT fk_codescheme_description;
ALTER TABLE codescheme_description ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE codescheme_changenote DROP CONSTRAINT fk_codescheme_changenote;
ALTER TABLE codescheme_changenote ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

-- Code

ALTER TABLE code DROP CONSTRAINT fk_code_codescheme;
ALTER TABLE code ADD CONSTRAINT fk_code_codescheme FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE code_preflabel DROP CONSTRAINT fk_code_preflabel;
ALTER TABLE code_preflabel ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE code_definition DROP CONSTRAINT fk_code_definition;
ALTER TABLE code_definition ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE code_description DROP CONSTRAINT fk_code_description;
ALTER TABLE code_description ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

-- ExternalReference

ALTER TABLE externalreference DROP CONSTRAINT fk_externalreference_parentcodescheme;
ALTER TABLE externalreference ADD CONSTRAINT fk_parentcodescheme_id FOREIGN KEY (parentcodescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE externalreference_title DROP CONSTRAINT fk_externalreference_title;
ALTER TABLE externalreference_title ADD CONSTRAINT fk_externalreference_id FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE externalreference_description DROP CONSTRAINT fk_externalreference_description;
ALTER TABLE externalreference_description ADD CONSTRAINT fk_externalreference_id FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

-- ExtensionScheme

ALTER TABLE extensionscheme_preflabel DROP CONSTRAINT fk_extensionscheme_preflabel;
ALTER TABLE extensionscheme_preflabel ADD CONSTRAINT fk_extensionscheme_id FOREIGN KEY (extensionscheme_id) REFERENCES extensionscheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

-- code_externalreference

ALTER TABLE code_externalreference DROP CONSTRAINT fk_code_externalreference;
ALTER TABLE code_externalreference ADD CONSTRAINT fk_code_externalreference FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

-- codescheme_externalreference

ALTER TABLE codescheme_externalreference DROP CONSTRAINT fk_codescheme_externalreference;
ALTER TABLE codescheme_externalreference ADD CONSTRAINT fk_codescheme_externalreference FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

-- EditedEntry

ALTER TABLE editedentry DROP CONSTRAINT fk_commit_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_commit_id FOREIGN KEY (commit_id) REFERENCES commit (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE editedentry DROP CONSTRAINT fk_code_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE editedentry DROP CONSTRAINT fk_extension_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_extension_id FOREIGN KEY (extension_id) REFERENCES extension (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE editedentry DROP CONSTRAINT fk_extensionscheme_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_extensionscheme_id FOREIGN KEY (extensionscheme_id) REFERENCES extensionscheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE editedentry DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE editedentry DROP CONSTRAINT fk_externalreference_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_externalreference_id FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

-- code_concept

ALTER TABLE code_concept DROP CONSTRAINT fk_code_id;
ALTER TABLE code_concept ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

-- codescheme_concept

ALTER TABLE codescheme_concept DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE codescheme_concept ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;
