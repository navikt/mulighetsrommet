ALTER TABLE tiltak
ADD COLUMN archived BOOLEAN NOT NULL  DEFAULT 'false',
ADD CONSTRAINT fk_region FOREIGN  KEY (region_id) REFERENCES  Region (id);