-- Renaming foreign key constraint to proper name.

ALTER TABLE member DROP CONSTRAINT fk_broadermember_id;
ALTER TABLE member ADD CONSTRAINT fk_relatedmember_id FOREIGN KEY (relatedmember_id) REFERENCES member(id);