-- Changing hierarchylevel from text to number

ALTER TABLE code DROP COLUMN hierarchylevel;
ALTER TABLE code ADD hierarchylevel integer NULL;
