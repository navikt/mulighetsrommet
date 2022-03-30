-- Remove constraint
ALTER TABLE tiltaksgjennomforing DROP CONSTRAINT fk_tiltakstype;

-- Add new column
ALTER TABLE tiltaksgjennomforing ADD COLUMN tiltakskode tiltakskode;
ALTER TABLE tiltaksgjennomforing
    ADD CONSTRAINT fk_tiltakskode FOREIGN KEY (tiltakskode) REFERENCES tiltakstype (tiltakskode);

-- Migrate rows
UPDATE tiltaksgjennomforing t1
SET tiltakskode = t2.tiltakskode
FROM tiltakstype t2
WHERE t1.id = t2.id;

-- Drop old column
ALTER TABLE tiltaksgjennomforing DROP COLUMN tiltakstype_id;
