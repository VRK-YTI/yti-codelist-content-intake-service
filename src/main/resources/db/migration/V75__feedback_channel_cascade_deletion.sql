-- Adding cascade delete support to codescheme with feedback channel

ALTER TABLE codescheme_feedback_channel DROP CONSTRAINT fk_codescheme_feedback_channel;
ALTER TABLE codescheme_feedback_channel ADD CONSTRAINT fk_codescheme_feedback_channel FOREIGN KEY (codescheme_id) REFERENCES codescheme(id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE;