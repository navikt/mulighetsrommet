drop view if exists avtale_admin_dto_view;

create type avtale_status as enum (
    'UTKAST',
    'AKTIV',
    'AVSLUTTET',
    'AVBRUTT');

alter table avtale
    add status avtale_status;

update avtale
set status = case
                 when avtale.avbrutt_tidspunkt is not null then 'AVBRUTT'
                 when avtale.slutt_dato is not null and date(now()) > avtale.slutt_dato then 'AVSLUTTET'
                 when avtale.arrangor_hovedenhet_id is null then 'UTKAST'
                 else 'AKTIV'
    end::avtale_status;

alter table avtale
    alter status set not null;
