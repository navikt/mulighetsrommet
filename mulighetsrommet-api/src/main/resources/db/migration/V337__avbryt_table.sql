drop view if exists gjennomforing_admin_dto_view;
drop view if exists avtale_admin_dto_view;

ALTER TABLE gjennomforing
    ALTER COLUMN avbrutt_aarsak TYPE text[] USING ARRAY[avbrutt_aarsak];

ALTER TABLE gjennomforing
    RENAME COLUMN avbrutt_aarsak TO avbrutt_aarsaker;

ALTER TABLE gjennomforing
    ADD COLUMN avbrutt_forklaring text;

ALTER TABLE avtale
    ALTER COLUMN avbrutt_aarsak TYPE text[] USING ARRAY[avbrutt_aarsak];

ALTER TABLE avtale
    RENAME COLUMN avbrutt_aarsak TO avbrutt_aarsaker;

ALTER TABLE avtale
    ADD COLUMN avbrutt_forklaring text;

