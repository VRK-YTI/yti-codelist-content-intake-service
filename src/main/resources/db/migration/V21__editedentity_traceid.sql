-- Adding trace_id as separate column to Commit

DELETE FROM commit;
DELETE FROM editedentity;

ALTER TABLE commit ALTER COLUMN id SET DATA TYPE uuid USING id::uuid;
ALTER TABLE commit ADD trace_id text NULL;

ALTER TABLE editedentity ALTER COLUMN commit_id SET DATA TYPE uuid USING id::uuid;