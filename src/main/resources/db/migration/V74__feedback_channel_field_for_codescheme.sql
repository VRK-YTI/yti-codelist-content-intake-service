CREATE TABLE codescheme_feedback_channel
(
    codescheme_id    uuid NOT NULL,
    language         text NOT NULL,
    feedback_channel text NOT NULL,
    CONSTRAINT codescheme_feedback_channel_pkey PRIMARY KEY (codescheme_id, language),
    CONSTRAINT fk_codescheme_feedback_channel FOREIGN KEY (codescheme_id) REFERENCES codescheme (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);