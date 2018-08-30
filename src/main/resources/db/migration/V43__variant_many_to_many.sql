UPDATE codescheme SET variant_codescheme_id = null;

CREATE TABLE public.codescheme_variant
(
    codescheme_id uuid NOT NULL,
    variant_codescheme_id uuid NOT NULL,
    CONSTRAINT codescheme_variant_codescheme_id_fk FOREIGN KEY (codescheme_id) REFERENCES codescheme (id),
    CONSTRAINT codescheme_variant_codescheme_id_fk_2 FOREIGN KEY (variant_codescheme_id) REFERENCES codescheme (id)
);
CREATE UNIQUE INDEX codescheme_id_variant_codescheme_id_unique_combo ON public.codescheme_variant (codescheme_id, variant_codescheme_id);

ALTER TABLE codescheme DROP COLUMN variant_codescheme_id;