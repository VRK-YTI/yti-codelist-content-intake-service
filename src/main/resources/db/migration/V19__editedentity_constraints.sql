-- Loosening up constraints for editedentity

ALTER TABLE editedentity DROP CONSTRAINT fk_commit_id;
ALTER TABLE editedentity DROP CONSTRAINT fk_codescheme_id;
ALTER TABLE editedentity DROP CONSTRAINT fk_code_id;
ALTER TABLE editedentity DROP CONSTRAINT fk_externalreference_id;
ALTER TABLE editedentity DROP CONSTRAINT fk_extension_id;
ALTER TABLE editedentity DROP CONSTRAINT fk_extensionscheme_id;

ALTER TABLE editedentity ADD CONSTRAINT fk_commit_id FOREIGN KEY (commit_id) REFERENCES commit (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE editedentity ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE editedentity ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE editedentity ADD CONSTRAINT fk_externalreference_id FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE editedentity ADD CONSTRAINT fk_extension_id FOREIGN KEY (extension_id) REFERENCES extension (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE editedentity ADD CONSTRAINT fk_extensionscheme_id FOREIGN KEY (extensionscheme_id) REFERENCES extensionscheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;