-- Migration for empty created and modified columns for coderegistry, codescheme, code, externalreference, propertytype, extensionscheme and extension

UPDATE coderegistry SET created = '2018-01-01', modified = '2018-01-01' WHERE created IS NULL AND modified IS NULL;
UPDATE coderegistry as cr SET modified = cr.created WHERE modified IS NULL;

UPDATE codescheme SET created = '2018-01-01', modified = '2018-01-01' WHERE created IS NULL AND modified IS NULL;
UPDATE codescheme as cs SET modified = cs.created WHERE modified IS NULL;

UPDATE code SET created = '2018-01-01', modified = '2018-01-01' WHERE created IS NULL AND modified IS NULL;
UPDATE code as c SET modified = c.created WHERE modified IS NULL;

UPDATE externalreference SET created = '2018-01-01', modified = '2018-01-01' WHERE created IS NULL AND modified IS NULL;
UPDATE externalreference as e SET modified = e.created WHERE modified IS NULL;

UPDATE propertytype SET created = '2018-01-01', modified = '2018-01-01' WHERE created IS NULL AND modified IS NULL;
UPDATE propertytype as p SET modified = p.created WHERE modified IS NULL;

UPDATE extensionscheme SET created = '2018-01-01', modified = '2018-01-01' WHERE created IS NULL AND modified IS NULL;
UPDATE extensionscheme as es SET modified = es.created WHERE modified IS NULL;

UPDATE extension SET created = '2018-01-01', modified = '2018-01-01' WHERE created IS NULL AND modified IS NULL;
UPDATE extension as e SET modified = e.created WHERE modified IS NULL;
