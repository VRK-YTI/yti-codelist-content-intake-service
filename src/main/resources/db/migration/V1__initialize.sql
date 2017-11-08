---

--- Generic YTI data model

-- CodeRegistry

CREATE TABLE coderegistry (
  id uuid UNIQUE NOT NULL,
  uri text UNIQUE NOT NULL,
  codevalue text NOT NULL,
  modified timestamp without time zone NOT NULL,
  CONSTRAINT coderegistry_pkey PRIMARY KEY (id)
);

CREATE TABLE coderegistry_preflabel (
  coderegistry_id uuid NOT NULL,
  language text NOT NULL,
  preflabel text NOT NULL,
  CONSTRAINT coderegistry_preflabel_pkey PRIMARY KEY (coderegistry_id, language),
  CONSTRAINT fk_coderegistry_preflabel FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE coderegistry_definition (
  coderegistry_id uuid NOT NULL,
  language text NOT NULL,
  definition text NOT NULL,
  CONSTRAINT coderegistry_definition_pkey PRIMARY KEY (coderegistry_id, language),
  CONSTRAINT fk_coderegistry_definition FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- CodeScheme

CREATE TABLE codescheme (
  id uuid UNIQUE NOT NULL,
  uri text UNIQUE NOT NULL,
  codevalue text NOT NULL,
  status text NOT NULL,
  version text NOT NULL,
  source text NOT NULL,
  legalbase text NULL,
  governancepolicy text NULL,
  license text NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  modified timestamp without time zone NOT NULL,
  coderegistry_id uuid NULL,
  CONSTRAINT codescheme_pkey PRIMARY KEY (id),
  CONSTRAINT fk_codescheme_coderegistry FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) ON DELETE CASCADE
);

CREATE TABLE codescheme_preflabel (
  codescheme_id uuid NOT NULL,
  language text NOT NULL,
  preflabel text NOT NULL,
  CONSTRAINT codescheme_preflabel_pkey PRIMARY KEY (codescheme_id, language),
  CONSTRAINT fk_codescheme_preflabel FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE codescheme_definition (
  codescheme_id uuid NOT NULL,
  language text NOT NULL,
  definition text NOT NULL,
  CONSTRAINT codescheme_definition_pkey PRIMARY KEY (codescheme_id, language),
  CONSTRAINT fk_codescheme_definition FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE codescheme_description (
  codescheme_id uuid NOT NULL,
  language text NOT NULL,
  description text NOT NULL,
  CONSTRAINT codescheme_description_pkey PRIMARY KEY (codescheme_id, language),
  CONSTRAINT fk_codescheme_description FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE codescheme_changenote (
  codescheme_id uuid NOT NULL,
  language text NOT NULL,
  changenote text NOT NULL,
  CONSTRAINT codescheme_changenote_pkey PRIMARY KEY (codescheme_id, language),
  CONSTRAINT fk_codescheme_changenote FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- Single Code

CREATE TABLE code (
  id uuid UNIQUE NOT NULL,
  uri text UNIQUE NULL,
  codevalue text NOT NULL,
  status text NOT NULL,
  shortname text NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  modified timestamp without time zone NOT NULL,
  codescheme_id uuid NOT NULL,
  broadercode_id uuid NULL,
  CONSTRAINT code_pkey PRIMARY KEY (id),
  CONSTRAINT fk_code_codescheme FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_broadercode_id FOREIGN KEY (broadercode_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE code_preflabel (
  code_id uuid NOT NULL,
  language text NOT NULL,
  preflabel text NOT NULL,
  CONSTRAINT code_preflabel_pkey PRIMARY KEY (code_id, language),
  CONSTRAINT fk_code_preflabel FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE code_definition (
  code_id uuid NOT NULL,
  language text NOT NULL,
  definition text NOT NULL,
  CONSTRAINT code_definition_pkey PRIMARY KEY (code_id, language),
  CONSTRAINT fk_code_definition FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE code_description (
  code_id uuid NOT NULL,
  language text NOT NULL,
  description text NOT NULL,
  CONSTRAINT code_description_pkey PRIMARY KEY (code_id, language),
  CONSTRAINT fk_code_description FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- Property Type

CREATE TABLE propertytype (
  id uuid UNIQUE NOT NULL,
  uri text UNIQUE NOT NULL,
  localname text UNIQUE NOT NULL,
  type text NOT NULL,
  CONSTRAINT propertytype_pkey PRIMARY KEY (id)
);

CREATE TABLE propertytype_preflabel (
  propertytype_id uuid NOT NULL,
  language text NOT NULL,
  preflabel text NOT NULL,
  CONSTRAINT propertytype_preflabel_pkey PRIMARY KEY (propertytype_id, language),
  CONSTRAINT fk_propertytype_preflabel FOREIGN KEY (propertytype_id) REFERENCES propertytype (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE propertytype_definition (
  propertytype_id uuid NOT NULL,
  language text NOT NULL,
  definition text NOT NULL,
  CONSTRAINT propertytype_definition_pkey PRIMARY KEY (propertytype_id, language),
  CONSTRAINT fk_propertytype_definition FOREIGN KEY (propertytype_id) REFERENCES propertytype (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- External Reference

CREATE TABLE externalreference (
  id uuid UNIQUE NOT NULL,
  url text NULL,
  propertytype_id uuid NOT NULL,
  CONSTRAINT externalreference_pkey PRIMARY KEY (id),
  CONSTRAINT fk_externalreference_propertytype FOREIGN KEY (propertytype_id) REFERENCES propertytype (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE code_externalreference (
  code_id uuid NOT NULL,
  externalreference_id uuid NOT NULL,
  CONSTRAINT code_externalreference_pkey PRIMARY KEY (code_id, externalreference_id),
  CONSTRAINT fk_code_externalreference FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_externalreference FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE codescheme_externalreference (
  codescheme_id uuid NOT NULL,
  externalreference_id uuid NOT NULL,
  CONSTRAINT codescheme_externalreference_pkey PRIMARY KEY (codescheme_id, externalreference_id),
  CONSTRAINT fk_codescheme_externalreference FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_externalreference FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE externalreference_title (
  externalreference_id uuid NOT NULL,
  language text NOT NULL,
  title text NOT NULL,
  CONSTRAINT externalreference_title_pkey PRIMARY KEY (externalreference_id, language),
  CONSTRAINT fk_externalreference_title FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE externalreference_description (
  externalreference_id uuid NOT NULL,
  language text NOT NULL,
  description text NOT NULL,
  CONSTRAINT externalreference_description_pkey PRIMARY KEY (externalreference_id, language),
  CONSTRAINT fk_externalreference_description FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- Organization

CREATE TABLE organization (
  id uuid UNIQUE NOT NULL,
  CONSTRAINT organization_pkey PRIMARY KEY (id)
);

CREATE TABLE organization_name (
  organization_id uuid NOT NULL,
  language text NOT NULL,
  name text NOT NULL,
  CONSTRAINT organization_name_pkey PRIMARY KEY (organization_id, language),
  CONSTRAINT fk_organization_name FOREIGN KEY (organization_id) REFERENCES organization (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE coderegistry_organization (
  codescheme_id uuid NULL,
  organization_id uuid NULL,
  CONSTRAINT coderegistry_organization_pkey PRIMARY KEY (codescheme_id, organization_id),
  CONSTRAINT fk_coderegistry_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_coderegistry_organization FOREIGN KEY (organization_id) REFERENCES organization (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- Extension

CREATE TABLE extensionscheme (
  id uuid UNIQUE NOT NULL,
  propertytypeid uuid NOT NULL,
  codeschemeid uuid NOT NULL,
  targetcodeschemeid uuid NOT NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  CONSTRAINT extensionscheme_pkey PRIMARY KEY (id)
);

CREATE TABLE extensionscheme_preflabel (
  extensionscheme_id uuid NOT NULL,
  language text NOT NULL,
  preflabel text NOT NULL,
  CONSTRAINT extensionscheme_preflabel_pkey PRIMARY KEY (extensionscheme_id, language),
  CONSTRAINT fk_extensionscheme_preflabel FOREIGN KEY (extensionscheme_id) REFERENCES extensionscheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE extension (
  id uuid UNIQUE NOT NULL,
  codeid uuid NOT NULL,
  extensionid uuid NULL,
  extensionschemeid uuid NOT NULL,
  extensionvalue text NULL,
  CONSTRAINT extension_pkey PRIMARY KEY (id),
  CONSTRAINT fk_codeid FOREIGN KEY (codeid) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_extensionid FOREIGN KEY (extensionid) REFERENCES extension (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_extensionschemeid FOREIGN KEY (extensionschemeid) REFERENCES extensionscheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- User

CREATE TABLE ytiuser (
  email text UNIQUE NOT NULL,
  firstname text NOT NULL,
  lastname text NOT NULL,
  superuser bool NOT NULL,
  removed bool NOT NULL,
  CONSTRAINT ytiuser_pkey PRIMARY KEY (email)
);

CREATE TABLE commit (
  id uuid NOT NULL,
  modified timestamp without time zone NULL,
  useremail text NOT NULL,
  CONSTRAINT commit_pkey PRIMARY KEY (id),
  CONSTRAINT fk_useremail FOREIGN KEY (useremail) REFERENCES ytiuser (email) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE user_organization (
  useremail text NOT NULL,
  organization_id uuid NOT NULL,
  role_id character(255),
  CONSTRAINT user_organization_pkey PRIMARY KEY (useremail, organization_id, role_id),
  CONSTRAINT fk_useremail FOREIGN KEY (useremail) REFERENCES ytiuser (email) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_organization_id FOREIGN KEY (organization_id) REFERENCES organization (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE editedentry (
  commit_id uuid UNIQUE NOT NULL,
  code_id uuid NULL,
  extension_id uuid NULL,
  extensionscheme_id uuid NULL,
  codescheme_id uuid NOT NULL,
  externalreference_id uuid NULL,
  CONSTRAINT editedentry_pkey PRIMARY KEY (commit_id, code_id, extension_id, extensionscheme_id, codescheme_id, externalreference_id),
  CONSTRAINT fk_commit_id FOREIGN KEY (commit_id) REFERENCES commit (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_extension_id FOREIGN KEY (extension_id) REFERENCES extension (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_extensionscheme_id FOREIGN KEY (extensionscheme_id) REFERENCES extensionscheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_externalreference_id FOREIGN KEY (externalreference_id) REFERENCES externalreference (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- Data update handling and bookkeeping

CREATE TABLE indexstatus (
  id uuid UNIQUE NOT NULL,
  indexName text NOT NULL,
  indexAlias text NOT NULL,
  status text NOT NULL,
  created timestamp without time zone NOT NULL,
  modified timestamp without time zone NOT NULL,
  CONSTRAINT indexstatus_pkey PRIMARY KEY (id)
);

CREATE TABLE updatestatus (
  id uuid UNIQUE NOT NULL,
  datatype text NOT NULL,
  source text NOT NULL,
  status text NOT NULL,
  version text NOT NULL,
  nextVersion text NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  modified timestamp without time zone NOT NULL,
  CONSTRAINT updatestatus_pkey PRIMARY KEY (id)
);

---