-- Adding not null constraint to externalreference href

ALTER TABLE externalreference ALTER COLUMN href SET NOT NULL;