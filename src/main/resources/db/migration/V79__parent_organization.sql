-- add column for parent organization id

ALTER TABLE organization ADD COLUMN parent_id UUID;
ALTER TABLE organization ADD CONSTRAINT fk_parent_id FOREIGN KEY (parent_id) REFERENCES organization(id);