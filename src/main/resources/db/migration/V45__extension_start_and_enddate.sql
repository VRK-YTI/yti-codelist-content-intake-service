-- Adding start and enddate to extension

ALTER TABLE extension ADD startdate date NULL;
ALTER TABLE extension ADD enddate date NULL;
