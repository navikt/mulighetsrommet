drop view if exists view_gjennomforing;

CREATE TYPE pamelding_type AS ENUM (
  'DIREKTE_VEDTAK',
  'TRENGER_GODKJENNING'
);

ALTER TABLE gjennomforing ADD COLUMN pamelding_type pamelding_type;

UPDATE gjennomforing
SET pamelding_type =
  CASE
    WHEN oppstart = 'FELLES' THEN 'TRENGER_GODKJENNING'::pamelding_type
    ELSE 'DIREKTE_VEDTAK'::pamelding_type
  END;

ALTER TABLE gjennomforing ALTER COLUMN pamelding_type SET NOT NULL;
