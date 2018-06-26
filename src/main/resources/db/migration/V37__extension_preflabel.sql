-- Adding prefLabel support to Extension

CREATE TABLE extension_preflabel (
  extension_id uuid NOT NULL,
  language text NOT NULL,
  preflabel text NOT NULL,
  CONSTRAINT extension_preflabel_pkey PRIMARY KEY (extension_id, language),
  CONSTRAINT fk_extension_id FOREIGN KEY (extension_id) REFERENCES extension (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);
