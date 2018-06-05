---

ALTER TABLE coderegistry_preflabel DROP CONSTRAINT fk_coderegistry_id;
ALTER TABLE coderegistry_preflabel ADD CONSTRAINT fk_coderegistry_id FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE coderegistry_definition DROP CONSTRAINT fk_coderegistry_id;
ALTER TABLE coderegistry_definition ADD CONSTRAINT fk_coderegistry_id FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE codescheme_preflabel DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE codescheme_preflabel ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE codescheme_definition DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE codescheme_definition ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE codescheme_description DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE codescheme_description ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE codescheme_changenote DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE codescheme_changenote ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE code_preflabel DROP CONSTRAINT fk_code_id;
ALTER TABLE code_preflabel ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE code_definition DROP CONSTRAINT fk_code_id;
ALTER TABLE code_definition ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE code_description DROP CONSTRAINT fk_code_id;
ALTER TABLE code_description ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE propertytype_preflabel DROP CONSTRAINT fk_propertytype_preflabel;
ALTER TABLE propertytype_preflabel ADD CONSTRAINT fk_propertytype_id FOREIGN KEY (propertytype_id) REFERENCES propertytype (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE propertytype_definition DROP CONSTRAINT fk_propertytype_definition;
ALTER TABLE propertytype_definition ADD CONSTRAINT fk_propertytype_id FOREIGN KEY (propertytype_id) REFERENCES propertytype (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE externalreference_title DROP CONSTRAINT fk_externalreference_id;
ALTER TABLE externalreference_title ADD CONSTRAINT fk_externalreference_id FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE externalreference_description DROP CONSTRAINT fk_externalreference_id;
ALTER TABLE externalreference_description ADD CONSTRAINT fk_externalreference_id FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE extensionscheme_preflabel DROP CONSTRAINT fk_extensionscheme_id;
ALTER TABLE extensionscheme_preflabel ADD CONSTRAINT fk_extensionscheme_id FOREIGN KEY (extensionscheme_id) REFERENCES extensionscheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;
