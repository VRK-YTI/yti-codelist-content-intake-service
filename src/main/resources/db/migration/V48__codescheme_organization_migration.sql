-- Migrating earlier organization information from coderegistry to codeschemes under them.

INSERT INTO codescheme_organization (codescheme_id, organization_id) SELECT id, organization_id FROM codescheme cs INNER JOIN coderegistry_organization cso ON cs.coderegistry_id = cso.coderegistry_id;