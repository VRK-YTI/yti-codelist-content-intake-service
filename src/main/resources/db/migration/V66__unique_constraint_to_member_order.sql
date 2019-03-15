ALTER TABLE member DROP CONSTRAINT IF  EXISTS unique_order;
ALTER TABLE member ADD CONSTRAINT unique_order UNIQUE (extension_id, memberorder);