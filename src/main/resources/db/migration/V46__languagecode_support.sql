-- CodeScheme linking to Language Code

CREATE TABLE languagecode_codescheme_code (
  codescheme_id uuid NOT NULL,
  code_id uuid NOT NULL,
  CONSTRAINT languagecode_codescheme_code_pkey PRIMARY KEY(codescheme_id, code_id)
);
