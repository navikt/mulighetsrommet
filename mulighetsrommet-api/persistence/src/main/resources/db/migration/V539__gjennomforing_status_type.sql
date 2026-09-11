drop view if exists view_arrangorflate_tiltak;
drop view if exists view_datavarehus_tiltak;
drop view if exists view_gjennomforing;
drop view if exists view_gjennomforing_avtale_detaljer;
drop view if exists view_gjennomforing_kompakt;
drop view if exists view_veilederflate_tiltak;

alter table gjennomforing
    alter status type text;

drop type gjennomforing_status;

-- Enkeltplasser speiler statusen til deltakeren sin, og får derfor en mer finkornet status enn tidligere.
-- Enkeltplasser uten deltaker havner i samme status som nyopprettede enkeltplasser.
update gjennomforing
set status = 'ENKELTPLASS_UTKAST_TIL_PAMELDING'
where gjennomforing_type = 'ENKELTPLASS';

update gjennomforing
set status = case deltaker.status_type
                 when 'KLADD' then 'ENKELTPLASS_UTKAST_TIL_PAMELDING'
                 when 'PABEGYNT_REGISTRERING' then 'ENKELTPLASS_UTKAST_TIL_PAMELDING'
                 when 'UTKAST_TIL_PAMELDING' then 'ENKELTPLASS_UTKAST_TIL_PAMELDING'
                 when 'SOKT_INN' then 'ENKELTPLASS_SOKT_INN'
                 when 'VURDERES' then 'ENKELTPLASS_SOKT_INN'
                 when 'VENTELISTE' then 'ENKELTPLASS_SOKT_INN'
                 when 'VENTER_PA_OPPSTART' then 'ENKELTPLASS_VENTER_PA_OPPSTART'
                 when 'DELTAR' then 'ENKELTPLASS_DELTAR'
                 when 'IKKE_AKTUELL' then 'ENKELTPLASS_IKKE_AKTUELL'
                 when 'FULLFORT' then 'ENKELTPLASS_FULLFORT'
                 when 'HAR_SLUTTET' then 'ENKELTPLASS_FULLFORT'
                 when 'AVBRUTT' then 'ENKELTPLASS_AVBRUTT'
                 when 'AVBRUTT_UTKAST' then 'ENKELTPLASS_AVBRUTT_UTKAST'
                 when 'FEILREGISTRERT' then 'ENKELTPLASS_FEILREGISTRERT'
                 else 'ENKELTPLASS_UTKAST_TIL_PAMELDING'
    end
from (select distinct on (gjennomforing_id) gjennomforing_id, status_type
      from deltaker
      order by gjennomforing_id) deltaker
where deltaker.gjennomforing_id = gjennomforing.id
  and gjennomforing.gjennomforing_type = 'ENKELTPLASS';

create table gjennomforing_status_type
(
    value text not null primary key
);

insert into gjennomforing_status_type(value)
values ('GJENNOMFORES'),
       ('AVSLUTTET'),
       ('AVBRUTT'),
       ('AVLYST'),
       ('ENKELTPLASS_UTKAST_TIL_PAMELDING'),
       ('ENKELTPLASS_SOKT_INN'),
       ('ENKELTPLASS_VENTER_PA_OPPSTART'),
       ('ENKELTPLASS_DELTAR'),
       ('ENKELTPLASS_IKKE_AKTUELL'),
       ('ENKELTPLASS_FULLFORT'),
       ('ENKELTPLASS_AVBRUTT'),
       ('ENKELTPLASS_AVBRUTT_UTKAST'),
       ('ENKELTPLASS_FEILREGISTRERT');

alter table gjennomforing
    add foreign key (status) references gjennomforing_status_type (value) on update cascade;
