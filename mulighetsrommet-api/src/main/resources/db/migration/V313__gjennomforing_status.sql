drop view if exists gjennomforing_admin_dto_view;
drop view if exists veilederflate_tiltak_view;

create type gjennomforing_status as enum (
    'GJENNOMFORES',
    'AVSLUTTET',
    'AVBRUTT',
    'AVLYST');

alter table gjennomforing
    add status gjennomforing_status;

update gjennomforing
set status = tiltaksgjennomforing_status(start_dato, slutt_dato, avsluttet_tidspunkt)::gjennomforing_status;

alter table gjennomforing
    alter status set not null;

drop function tiltaksgjennomforing_status;
