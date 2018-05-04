-- Adding coderegistry to editedentity

ALTER TABLE editedentity ADD coderegistry_id uuid NULL;
ALTER TABLE editedentity ADD propertytype_id uuid NULL;
