-- Correcting constraints in extensionscheme_codescheme

ALTER TABLE extensionscheme_codescheme DROP CONSTRAINT fk_extensionscheme;
ALTER TABLE extensionscheme_codescheme DROP CONSTRAINT fk_codescheme;
ALTER TABLE extensionscheme_codescheme ADD CONSTRAINT fk_extensionscheme_id FOREIGN KEY (extensionscheme_id) REFERENCES extensionscheme (id);
ALTER TABLE extensionscheme_codescheme ADD CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id);

-- Adding propertytype constraint to extensionscheme

ALTER TABLE extensionscheme ADD CONSTRAINT fk_propertytype FOREIGN KEY (propertytype_id) REFERENCES propertytype (id);

-- Dropping codescheme constraint for extensionscheme

ALTER TABLE extensionscheme DROP COLUMN codescheme_id;