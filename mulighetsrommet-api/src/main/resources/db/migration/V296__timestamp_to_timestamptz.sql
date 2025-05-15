drop view if exists gjennomforing_admin_dto_view;
drop view if exists tilsagn_admin_dto_view;
drop view if exists utbetaling_dto_view;

alter table arrangor
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table avtale
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table avtale_administrator
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table avtale_arrangor_underenhet
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table avtale_nav_enhet
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table avtale_opsjon_logg
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table del_med_bruker
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table deltaker
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table deltaker_registrering_innholdselement
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table gjennomforing
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table gjennomforing_administrator
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table gjennomforing_koordinator
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table gjennomforing_nav_enhet
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table lagret_filter
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table nav_ansatt
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table nav_ansatt_rolle
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table nav_ansatt_rolle_nav_enhet
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table notification
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo';

alter table tilsagn
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo';

alter table tiltakstype
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table tiltakstype_deltaker_registrering_innholdselement
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table utbetaling
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo';

alter table delutbetaling
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo';

alter table utdanning
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table utdanningsprogram
    alter created_at type timestamptz using created_at at time zone 'Europe/Oslo',
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';

alter table veileder_joyride
    alter updated_at type timestamptz using updated_at at time zone 'Europe/Oslo';
