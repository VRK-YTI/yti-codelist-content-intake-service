-- Migration and rename to extensionscheme to extension.

ALTER TABLE extensionscheme RENAME TO extension;

ALTER TABLE member RENAME extensionscheme_id TO extension_id;
ALTER TABLE member DROP CONSTRAINT fk_extensionscheme_id;
ALTER TABLE member ADD CONSTRAINT fk_extension_id FOREIGN KEY (extension_id) REFERENCES extension(id) ON DELETE CASCADE;

ALTER TABLE editedentity RENAME extensionscheme_id TO extension_id;

ALTER TABLE extensionscheme_preflabel RENAME TO extension_preflabel;
ALTER TABLE extension_preflabel RENAME extensionscheme_id TO extension_id;

ALTER TABLE extensionscheme_codescheme RENAME TO extension_codescheme;
ALTER TABLE extension_codescheme RENAME extensionscheme_id TO extension_id;