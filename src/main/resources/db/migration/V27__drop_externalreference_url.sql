-- Drop uri from externalreference and propertytype as unnecessary

ALTER TABLE externalreference DROP COLUMN uri;
ALTER TABLE propertytype DROP COLUMN uri;

-- Rename referenceurl to href

ALTER TABLE externalreference RENAME COLUMN referenceurl TO href;