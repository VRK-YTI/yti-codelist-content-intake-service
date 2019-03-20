ALTER TABLE code DROP CONSTRAINT IF EXISTS unique_flatorder;
ALTER TABLE code ADD CONSTRAINT unique_flatorder UNIQUE (codescheme_id, flatorder);