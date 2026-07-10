drop view if exists avtale_admin_dto_view;

ALTER TABLE avtale_sats
    ADD COLUMN gjelder_fra date;

UPDATE avtale_sats
SET gjelder_fra = lower(periode);

ALTER TABLE avtale_sats
    DROP COLUMN periode;
