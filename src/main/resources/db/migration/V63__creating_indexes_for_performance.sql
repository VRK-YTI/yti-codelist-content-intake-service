-- Adding indexes to optimize performmance based on analysis

CREATE INDEX CONCURRENTLY ON member (code_id);
CREATE INDEX CONCURRENTLY ON member (extension_id);
CREATE INDEX CONCURRENTLY ON membervalue (member_id);
CREATE INDEX CONCURRENTLY ON code (subcodescheme_id);
CREATE INDEX CONCURRENTLY ON code (codescheme_id, flatorder);
