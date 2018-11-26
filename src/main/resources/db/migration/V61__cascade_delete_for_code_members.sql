-- Adding cascade delete constraint support for code deletion

ALTER TABLE member DROP CONSTRAINT fk_code_id;
ALTER TABLE member ADD CONSTRAINT fk_code_id FOREIGN KEY (code_id) REFERENCES code(id) ON DELETE CASCADE;