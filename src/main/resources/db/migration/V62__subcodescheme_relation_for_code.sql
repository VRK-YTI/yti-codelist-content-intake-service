-- Adding SubCodeScheme relation to Code

ALTER TABLE code ADD COLUMN subcodescheme_id uuid NULL;
ALTER TABLE code ADD CONSTRAINT fk_subcodescheme_id FOREIGN KEY (subcodescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;