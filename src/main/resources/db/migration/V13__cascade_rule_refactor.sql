-- Adding proper CASCADE deletion rules

-- CodeRegistry

ALTER TABLE codescheme DROP CONSTRAINT fk_codescheme_coderegistry;
ALTER TABLE codescheme ADD CONSTRAINT fk_codescheme_coderegistry FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id);

ALTER TABLE coderegistry_preflabel DROP CONSTRAINT fk_coderegistry_id;
ALTER TABLE coderegistry_preflabel ADD CONSTRAINT fk_coderegistry_id FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id);

ALTER TABLE coderegistry_definition DROP CONSTRAINT fk_coderegistry_id;
ALTER TABLE coderegistry_definition ADD CONSTRAINT fk_coderegistry_id FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id);

-- CodeScheme

ALTER TABLE codescheme_preflabel DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE codescheme_preflabel ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id);

ALTER TABLE codescheme_definition DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE codescheme_definition ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id);

ALTER TABLE codescheme_description DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE codescheme_description ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id);

ALTER TABLE codescheme_changenote DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE codescheme_changenote ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id);

-- Code

ALTER TABLE code DROP CONSTRAINT fk_code_codescheme;
ALTER TABLE code ADD CONSTRAINT fk_code_codescheme FOREIGN KEY (codescheme_id) REFERENCES codescheme (id);

ALTER TABLE code_preflabel DROP CONSTRAINT fk_code_id;
ALTER TABLE code_preflabel ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id);

ALTER TABLE code_definition DROP CONSTRAINT fk_code_id;
ALTER TABLE code_definition ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id);

ALTER TABLE code_description DROP CONSTRAINT fk_code_id;
ALTER TABLE code_description ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id);

-- ExternalReference

ALTER TABLE externalreference DROP CONSTRAINT fk_parentcodescheme_id;
ALTER TABLE externalreference ADD CONSTRAINT fk_parentcodescheme_id FOREIGN KEY (parentcodescheme_id) REFERENCES codescheme (id);

ALTER TABLE externalreference_title DROP CONSTRAINT fk_externalreference_id;
ALTER TABLE externalreference_title ADD CONSTRAINT fk_externalreference_id FOREIGN KEY (externalreference_id) REFERENCES externalreference (id);

ALTER TABLE externalreference_description DROP CONSTRAINT fk_externalreference_id;
ALTER TABLE externalreference_description ADD CONSTRAINT fk_externalreference_id FOREIGN KEY (externalreference_id) REFERENCES externalreference (id);

-- ExtensionScheme

ALTER TABLE extensionscheme_preflabel DROP CONSTRAINT fk_extensionscheme_id;
ALTER TABLE extensionscheme_preflabel ADD CONSTRAINT fk_extensionscheme_id FOREIGN KEY (extensionscheme_id) REFERENCES extensionscheme (id);

-- code_externalreference

ALTER TABLE code_externalreference DROP CONSTRAINT fk_code_externalreference;
ALTER TABLE code_externalreference ADD CONSTRAINT fk_code_externalreference FOREIGN KEY (code_id) REFERENCES code (id);

-- codescheme_externalreference

ALTER TABLE codescheme_externalreference DROP CONSTRAINT fk_codescheme_externalreference;
ALTER TABLE codescheme_externalreference ADD CONSTRAINT fk_codescheme_externalreference FOREIGN KEY (codescheme_id) REFERENCES codescheme (id);

-- EditedEntry

ALTER TABLE editedentry DROP CONSTRAINT fk_commit_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_commit_id FOREIGN KEY (commit_id) REFERENCES commit (id);
ALTER TABLE editedentry DROP CONSTRAINT fk_code_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id);
ALTER TABLE editedentry DROP CONSTRAINT fk_extension_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_extension_id FOREIGN KEY (extension_id) REFERENCES extension (id);
ALTER TABLE editedentry DROP CONSTRAINT fk_extensionscheme_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_extensionscheme_id FOREIGN KEY (extensionscheme_id) REFERENCES extensionscheme (id);
ALTER TABLE editedentry DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id);
ALTER TABLE editedentry DROP CONSTRAINT fk_externalreference_id;
ALTER TABLE editedentry ADD CONSTRAINT fk_externalreference_id FOREIGN KEY (externalreference_id) REFERENCES externalreference (id);

-- code_concept

ALTER TABLE code_concept DROP CONSTRAINT fk_code_id;
ALTER TABLE code_concept ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id);

-- codescheme_concept

ALTER TABLE codescheme_concept DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE codescheme_concept ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id);
