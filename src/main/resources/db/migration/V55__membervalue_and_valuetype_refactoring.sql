-- Adding membervalue and valuetype tables and relations

CREATE TABLE valuetype (
  id uuid UNIQUE NOT NULL,
  localname text UNIQUE NOT NULL,
  valuetypeuri text NOT NULL,
  typeuri text NOT NULL,
  regexp text NULL,
  required bool NOT NULL,
  CONSTRAINT valuetype_pkey PRIMARY KEY (id)
);

CREATE TABLE valuetype_preflabel (
  valuetype_id uuid NOT NULL,
  language text NOT NULL,
  preflabel text NOT NULL,
  CONSTRAINT valuetype_preflabel_pkey PRIMARY KEY (valuetype_id, language),
  CONSTRAINT fk_valuetype_id FOREIGN KEY (valuetype_id) REFERENCES valuetype (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE propertytype_valuetype (
  propertytype_id uuid NOT NULL,
  valuetype_id uuid NOT NULL,
  CONSTRAINT propertytype_valuetype_pkey PRIMARY KEY(propertytype_id, valuetype_id)
);

CREATE TABLE membervalue (
  id uuid UNIQUE NOT NULL,
  value text NOT NULL,
  created timestamp without time zone NOT NULL,
  modified timestamp without time zone NOT NULL,
  valuetype_id uuid NOT NULL,
  member_id uuid NOT NULL,
  CONSTRAINT membervalue_pkey PRIMARY KEY (id),
  CONSTRAINT fk_valuetype_id FOREIGN KEY (valuetype_id) REFERENCES valuetype (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_member_id FOREIGN KEY (member_id) REFERENCES member (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

ALTER TABLE editedentity ADD COLUMN valuetype_id uuid NULL;
