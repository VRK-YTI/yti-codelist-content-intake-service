-- Migrating created date with latest modified in case it is null

UPDATE coderegistry as cr SET created = cr.modified WHERE created IS NULL;

UPDATE codescheme as cs SET created = cs.modified WHERE created IS NULL;

UPDATE code as c SET created = c.modified WHERE created IS NULL;

UPDATE externalreference as e SET created = e.modified WHERE created IS NULL;

UPDATE propertytype as p SET created = p.modified WHERE created IS NULL;

UPDATE extensionscheme as es SET created = es.modified WHERE created IS NULL;

UPDATE extension as e SET created = e.modified WHERE created IS NULL;
