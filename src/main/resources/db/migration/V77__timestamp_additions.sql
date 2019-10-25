-- Adding various timestamps to database

ALTER TABLE codescheme ADD content_modified timestamp without time zone NULL;

ALTER TABLE codescheme ADD status_modified timestamp without time zone NULL;
ALTER TABLE code ADD status_modified timestamp without time zone NULL;
ALTER TABLE extension ADD status_modified timestamp without time zone NULL;
