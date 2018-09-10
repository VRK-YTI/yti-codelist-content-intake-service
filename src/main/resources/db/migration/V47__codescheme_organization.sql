-- Adding organization hooks to codescheme directly

CREATE TABLE codescheme_organization (
  codescheme_id uuid NULL,
  organization_id uuid NULL,
  CONSTRAINT codescheme_organization_pkey PRIMARY KEY (codescheme_id, organization_id),
  CONSTRAINT fk_codescheme_id FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_organization_id FOREIGN KEY (organization_id) REFERENCES organization (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);
