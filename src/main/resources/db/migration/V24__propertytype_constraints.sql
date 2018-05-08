-- PropertyType unique constraint refactored to take into account Context

ALTER TABLE propertytype DROP CONSTRAINT propertytype_localname_key;
ALTER TABLE propertytype DROP CONSTRAINT propertytype_propertyuri_key;

ALTER TABLE propertytype ADD UNIQUE (localname, context);
