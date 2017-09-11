---

--- Generic YTI data model

-- CodeRegistry

CREATE TABLE coderegistry (
  id character varying(255) UNIQUE NOT NULL,
  codevalue character varying(255) NOT NULL,
  status character varying(255) NULL,
  uri character varying(2048) NULL,
  definition character varying(4096) NULL,
  source character varying(255) NOT NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  CONSTRAINT coderegistry_pkey PRIMARY KEY (id)
);

CREATE TABLE coderegistry_preflabel (
  coderegistry_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  preflabel character varying(2048) NOT NULL,
  CONSTRAINT fk_coderegistry_preflabel FOREIGN KEY (coderegistry_id) REFERENCES coderegistry (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- CodeScheme

CREATE TABLE codescheme (
  id character varying(255) UNIQUE NOT NULL,
  codevalue character varying(255) NOT NULL,
  status character varying(255) NOT NULL,
  type character varying(255) NOT NULL,
  version character varying(255) NOT NULL,
  uri character varying(2048) NULL,
  description character varying(4096) NULL,
  definition character varying(4096) NULL,
  changenote character varying(4096) NULL,
  source character varying(255) NOT NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NOT NULL,
  modified timestamp without time zone NULL,
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


-- Single Code

CREATE TABLE code (
  id character varying(255) UNIQUE NOT NULL,
  codevalue character varying(255) NOT NULL,
  status character varying(255) NOT NULL,
  uri character varying(2048) NULL,
  description character varying(4096) NULL,
  definition character varying(4096) NULL,
  shortName character varying(1024) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NOT NULL,
  modified timestamp without time zone NULL,
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


-- Data update handling and bookkeeping

CREATE TABLE updatestatus (
  id character varying(255) UNIQUE NOT NULL,
  datatype character varying(255) NOT NULL,
  source character varying(255) NOT NULL,
  status character varying(255) NOT NULL,
  version character varying(255) NOT NULL,
  nextVersion character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  CONSTRAINT updatestatus_pkey PRIMARY KEY (id)
);

--- Custom legacy "CodeSchemes"

-- HealthCareDistrict

CREATE TABLE healthcaredistrict (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  codevalue character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  specialareaofresponsibility character varying(255) NULL,
  abbreviation character varying(255) NULL,
  CONSTRAINT healthcaredistrict_pkey PRIMARY KEY (id)
);

-- Magistrate

CREATE TABLE magistrate (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  codevalue character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  CONSTRAINT magistrate_pkey PRIMARY KEY (id)
);

-- Region

CREATE TABLE region (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  codevalue character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  CONSTRAINT region_pkey PRIMARY KEY (id)
);

-- ElectoralDistrict

CREATE TABLE electoraldistrict (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  codevalue character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  CONSTRAINT electoraldistrict_pkey PRIMARY KEY (id)
);

-- BusinessServiceSubRegion

CREATE TABLE businessservicesubregion (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  codevalue character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  CONSTRAINT businessservicesubregion_pkey PRIMARY KEY (id)
);

-- PostManagementDistricts

CREATE TABLE postmanagementdistrict (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  codevalue character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  CONSTRAINT postmanagementdistrict_pkey PRIMARY KEY (id)
);

-- MagistrateServiceUnits

CREATE TABLE magistrateserviceunit (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  codevalue character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  CONSTRAINT magistrateserviceunit_pkey PRIMARY KEY (id)
);

-- Municipalities

CREATE TABLE municipality (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  codevalue character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  language character varying(255) NULL,
  type character varying(255) NULL,
  region_id character varying(255) NULL,
  magistrate_id character varying(255) NULL,
  healthcaredistrict_id character varying(255) NULL,
  magistrateserviceunit_id character varying(255) NULL,
  businessservicesubregion_id character varying(255) NULL,
  electoraldistrict_id character varying(255) NULL,
  CONSTRAINT municipality_pkey PRIMARY KEY (id),
  CONSTRAINT fk_municipality_healthcaredistrict FOREIGN KEY (healthcaredistrict_id) REFERENCES healthcaredistrict(id) ON DELETE CASCADE,
  CONSTRAINT fk_municipality_electoraldistrict FOREIGN KEY (electoraldistrict_id) REFERENCES electoraldistrict(id) ON DELETE CASCADE,
  CONSTRAINT fk_municipality_businessservicesubregion FOREIGN KEY (businessservicesubregion_id) REFERENCES businessservicesubregion(id) ON DELETE CASCADE,
  CONSTRAINT fk_municipality_magistrateserviceunit FOREIGN KEY (magistrateserviceunit_id) REFERENCES magistrateserviceunit(id) ON DELETE CASCADE
);

CREATE TABLE municipality_language (
  municipality_id character varying(255) NOT NULL,
  language character varying(255) NOT NULL,
  CONSTRAINT fk_municipality_language_municipality FOREIGN KEY (municipality_id)
  REFERENCES municipality (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- PostalCodes

CREATE TABLE postalcode (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  codevalue character varying(255) UNIQUE NOT NULL,
  uri character varying(255) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  nameabbr_fi character varying(255) NULL,
  nameabbr_se character varying(255) NULL,
  nameabbr_en character varying(255) NULL,
  typecode character varying(255) NULL,
  typename character varying(255) NULL,
  municipality_id character varying(255) NULL,
  postmanagementdistrict_id character varying(255) NULL,
  CONSTRAINT postalcode_pkey PRIMARY KEY (id),
  CONSTRAINT fk_postalcode_municipality FOREIGN KEY (municipality_id) REFERENCES municipality(id) ON DELETE CASCADE,
  CONSTRAINT fk_postalcode_postmanagementdistrict FOREIGN KEY (postmanagementdistrict_id) REFERENCES postmanagementdistrict(id) ON DELETE CASCADE
);

-- StreetAddresses and StreetNumbers

CREATE TABLE streetaddress (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  municipality_id character varying(255) NULL,
  CONSTRAINT streetaddress_pkey PRIMARY KEY (id),
  CONSTRAINT fk_streetaddress_municipality FOREIGN KEY (municipality_id) REFERENCES municipality(id) ON DELETE CASCADE
);

CREATE TABLE streetnumber (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  uri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  iseven boolean NULL,
  streetaddress_id character varying(255) NULL,
  postalcode_id character varying(255) NULL,
  startnumber integer NULL,
  endnumber integer NULL,
  startnumberend integer NULL,
  endnumberend integer NULL,
  startcharacter character varying(1) NULL,
  endcharacter character varying(1) NULL,
  startcharacterend character varying(1) NULL,
  endcharacterend character varying(1) NULL,
  CONSTRAINT streetnumber_pkey PRIMARY KEY (id),
  CONSTRAINT fk_streetnumber_streetaddress FOREIGN KEY (streetaddress_id) REFERENCES streetaddress(id) ON DELETE CASCADE,
  CONSTRAINT fk_streetnumber_postalcode FOREIGN KEY (postalcode_id) REFERENCES postalcode(id) ON DELETE CASCADE
);

-- BusinessID

CREATE TABLE businessid (
  id character varying(255) UNIQUE NOT NULL,
  status character varying(255) NOT NULL,
  codevalue character varying(255) UNIQUE NOT NULL,
  uri character varying(2048) NULL,
  detailsuri character varying(2048) NULL,
  source character varying(255) NULL,
  startdate timestamp without time zone NULL,
  enddate timestamp without time zone NULL,
  created timestamp without time zone NULL,
  modified timestamp without time zone NULL,
  preflabel_fi character varying(255) NULL,
  preflabel_se character varying(255) NULL,
  preflabel_en character varying(255) NULL,
  companyform character varying(255) NULL,
  CONSTRAINT businessid_pkey PRIMARY KEY (id)
);

---