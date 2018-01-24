-- Adding duplicate value index "constraints" to CodeScheme and Code tables

CREATE UNIQUE INDEX code_codevalue_unique_per_codescheme ON public.code (codevalue, codescheme_id);
CREATE UNIQUE INDEX codescheme_codevalue_unique_per_coderegistry ON public.codescheme (codevalue, coderegistry_id);