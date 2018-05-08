-- Dropping unnecessary columns

ALTER TABLE extensionscheme DROP COLUMN targetcodescheme_id;

-- Adding status to extensionscheme

ALTER TABLE extensionscheme ADD COLUMN status text NOT NULL;

-- Adding codevalue to extensionscheme

ALTER TABLE extensionscheme ADD COLUMN codevalue text NOT NULL;

-- Adding order to extension

ALTER TABLE extension ADD COLUMN extensionorder integer NULL;

-- Adding manytomany table

CREATE TABLE extensionscheme_codescheme (
  extensionscheme_id uuid NOT NULL,
  codescheme_id uuid NOT NULL,
  CONSTRAINT extensionscheme_codescheme_pkey PRIMARY KEY (extensionscheme_id, codescheme_id),
  CONSTRAINT fk_extensionscheme FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_codescheme FOREIGN KEY (extensionscheme_id) REFERENCES extensionscheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);
