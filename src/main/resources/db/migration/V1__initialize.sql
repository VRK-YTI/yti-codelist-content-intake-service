---

--- Generic YTI data model

-- CodeRegistry

CREATE TABLE coderegistry (
  id character varying(255) UNIQUE NOT NULL,
  codevalue character varying(255) NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NOT NULL,
  modified timestamp without time zone NOT NULL,
  CONSTRAINT coderegistry_pkey PRIMARY KEY (id)
);

CREATE TABLE coderegistry_preflabel (
  coderegistry_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  preflabel character varying(2048) NOT NULL,
  CONSTRAINT fk_coderegistry_preflabel FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE coderegistry_definition (
  coderegistry_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  definition character varying(2048) NOT NULL,
  CONSTRAINT fk_coderegistry_definition FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- CodeScheme

CREATE TABLE codescheme (
  id character varying(255) UNIQUE NOT NULL,
  codevalue character varying(255) NOT NULL,
  status character varying(255) NOT NULL,
  version character varying(255) NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NOT NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  modified timestamp without time zone NOT NULL,
  coderegistry_id character varying(255) NULL,
  CONSTRAINT codescheme_pkey PRIMARY KEY (id),
  CONSTRAINT fk_codescheme_coderegistry FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) ON DELETE CASCADE
);

CREATE TABLE codescheme_preflabel (
  codescheme_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  preflabel character varying(2048) NOT NULL,
  CONSTRAINT fk_codescheme_preflabel FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE codescheme_definition (
  codescheme_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  definition character varying(2048) NOT NULL,
  CONSTRAINT fk_codescheme_definition FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE codescheme_description (
  codescheme_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  description character varying(2048) NOT NULL,
  CONSTRAINT fk_codescheme_description FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE codescheme_changenote (
  codescheme_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  changenote character varying(2048) NOT NULL,
  CONSTRAINT fk_codescheme_changenote FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- Single Code

CREATE TABLE code (
  id character varying(255) UNIQUE NOT NULL,
  codevalue character varying(255) NOT NULL,
  status character varying(255) NOT NULL,
  uri character varying(2048) NULL,
  shortname character varying(1024) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  modified timestamp without time zone NOT NULL,
  codescheme_id character varying(255) NULL,
  CONSTRAINT code_pkey PRIMARY KEY (id),
  CONSTRAINT fk_code_codescheme FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) ON DELETE CASCADE
);

CREATE TABLE code_preflabel (
  code_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  preflabel character varying(2048) NOT NULL,
  CONSTRAINT fk_code_preflabel FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE code_definition (
  code_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  definition character varying(2048) NOT NULL,
  CONSTRAINT fk_code_definition FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE code_description (
  code_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  description character varying(2048) NOT NULL,
  CONSTRAINT fk_code_description FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- Data update handling and bookkeeping

CREATE TABLE indexstatus (
  id character varying(255) UNIQUE NOT NULL,
  indexName character varying(255) NOT NULL,
  indexAlias character varying(255) NOT NULL,
  status character varying(255) NOT NULL,
  created timestamp without time zone NOT NULL,
  modified timestamp without time zone NULL,
  CONSTRAINT indexstatus_pkey PRIMARY KEY (id)
);

CREATE TABLE updatestatus (
  id character varying(255) UNIQUE NOT NULL,
  datatype character varying(255) NOT NULL,
  source character varying(255) NOT NULL,
  status character varying(255) NOT NULL,
  version character varying(255) NOT NULL,
  nextVersion character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  modified timestamp without time zone NOT NULL,
  CONSTRAINT updatestatus_pkey PRIMARY KEY (id)
);

---