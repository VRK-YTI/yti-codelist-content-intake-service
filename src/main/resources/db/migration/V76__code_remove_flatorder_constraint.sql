-- Removing unique_flatorder constraint from code table due to transactional issues.

ALTER TABLE code DROP CONSTRAINT IF EXISTS unique_flatorder;