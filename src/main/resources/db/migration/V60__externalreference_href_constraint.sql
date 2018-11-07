-- Adding href constraint to externalreference so that same link can exist only once / codescheme

ALTER TABLE externalreference ADD CONSTRAINT uq_externalreference_parentcodescheme UNIQUE(parentcodescheme_id, href);