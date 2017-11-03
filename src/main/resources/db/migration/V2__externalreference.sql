-- Property Type

CREATE TABLE propertytype (
  id character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) UNIQUE NOT NULL,
  notation character varying(2048) NULL,
  modified timestamp without time zone NOT NULL
);

CREATE TABLE propertytype_preflabel (
  propertytype_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  preflabel character varying(2048) NOT NULL,
  CONSTRAINT fk_propertytype_preflabel FOREIGN KEY (propertytype_id) REFERENCES propertytype (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE propertytype_definition (
  propertytype_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  definition character varying(2048) NOT NULL,
  CONSTRAINT fk_propertytype_definition FOREIGN KEY (propertytype_id) REFERENCES propertytype (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- External References

CREATE TABLE externalreference (
  id character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) NULL,
  propertytype_id character varying(255) NOT NULL,
  CONSTRAINT fk_externalreference_propertytype FOREIGN KEY (propertytype_id) REFERENCES propertytype (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE code_externalreference (
  code_id character varying(255) NULL,
  externalreference_id character varying(255) NULL,
  CONSTRAINT fk_code_externalreference FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_externalreference FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE codescheme_externalreference (
  codescheme_id character varying(255) NULL,
  externalreference_id character varying(255) NULL,
  CONSTRAINT fk_codescheme_externalreference FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_externalreference FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE externalreference_title (
  language character varying(255) NOT NULL,
  title character varying(2048) NOT NULL,
  externalreference_id character varying(255) NULL,
  CONSTRAINT fk_externalreference_title FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE externalreference_description (
  language character varying(255) NOT NULL,
  description character varying(2048) NOT NULL,
  externalreference_id character varying(255) NULL,
  CONSTRAINT fk_externalreference_description FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);
