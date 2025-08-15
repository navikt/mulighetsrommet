drop view if exists utbetaling_dto_view;

ALTER TYPE totrinnskontroll_type RENAME TO totrinnskontroll_type_old;
CREATE TYPE totrinnskontroll_type AS ENUM (
    'GJOR_OPP',
    'ANNULLER',
    'OPPRETT'
);

ALTER TABLE totrinnskontroll ALTER COLUMN type TYPE totrinnskontroll_type USING type::text::totrinnskontroll_type;

DROP TYPE totrinnskontroll_type_old;

ALTER TYPE utbetaling_status RENAME TO utbetaling_status_old;
CREATE TYPE utbetaling_status AS ENUM (
     'GENERERT',
     'INNSENDT',
     'TIL_ATTESTERING',
     'RETURNERT',
     'FERDIG_BEHANDLET'
);

ALTER TABLE utbetaling
  ALTER COLUMN status TYPE utbetaling_status
  USING status::text::utbetaling_status;

DROP TYPE utbetaling_status_old;
