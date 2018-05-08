-- Bringing back localname constraint for PropertyType

ALTER TABLE propertytype DROP CONSTRAINT propertytype_localname_context_key;
ALTER TABLE propertytype ADD CONSTRAINT propertytype_localname_key UNIQUE (localname);
