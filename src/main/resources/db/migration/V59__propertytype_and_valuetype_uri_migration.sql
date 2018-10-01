-- Renaming fields to uri in valuetype and propertytype tables

ALTER TABLE valuetype RENAME COLUMN valuetypeuri TO uri;
ALTER TABLE propertytype RENAME COLUMN propertyuri TO uri;