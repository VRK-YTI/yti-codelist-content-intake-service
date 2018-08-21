ALTER TABLE public.codescheme
ADD CONSTRAINT codescheme_fk_prev_version
FOREIGN KEY (prev_codescheme_id) REFERENCES codescheme (id);

ALTER TABLE public.codescheme
ADD CONSTRAINT codescheme_fk_next_version
FOREIGN KEY (next_codescheme_id) REFERENCES codescheme (id);

ALTER TABLE public.codescheme
ADD CONSTRAINT codescheme_fk_last_version
FOREIGN KEY (last_codescheme_id) REFERENCES codescheme (id);