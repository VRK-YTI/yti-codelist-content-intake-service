-- Adding duplicate value index "constraints" to CodeScheme and Code tables

DROP INDEX IF EXISTS code_codevalue_unique_per_codescheme;
DROP INDEX IF EXISTS codescheme_codevalue_unique_per_coderegistry;
CREATE UNIQUE INDEX code_codevalue_unique_per_codescheme ON public.code (codevalue, codescheme_id);
CREATE UNIQUE INDEX codescheme_codevalue_unique_per_coderegistry ON public.codescheme (codevalue, coderegistry_id);