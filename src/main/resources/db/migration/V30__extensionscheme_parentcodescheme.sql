-- Adding parentcodescheme to extensionscheme

ALTER TABLE extensionscheme ADD COLUMN parentcodescheme_id uuid NOT NULL;
ALTER TABLE extensionscheme ADD CONSTRAINT parentcodescheme_id FOREIGN KEY (parentcodescheme_id) REFERENCES codescheme (id);