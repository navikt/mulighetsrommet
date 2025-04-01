-- ${flyway:timestamp}

drop view if exists view_nav_ansatt_dto;

create view view_nav_ansatt_dto as
select nav_ansatt.nav_ident,
       nav_ansatt.fornavn,
       nav_ansatt.etternavn,
       nav_ansatt.azure_id,
       nav_ansatt.mobilnummer,
       nav_ansatt.epost,
       nav_ansatt.skal_slettes_dato,
       nav_enhet.enhetsnummer as hovedenhet_enhetsnummer,
       nav_enhet.navn         as hovedenhet_navn,
       array_agg(rolle.rolle) as roller
from nav_ansatt
         join nav_enhet on nav_ansatt.hovedenhet = nav_enhet.enhetsnummer
         left join nav_ansatt_rolle rolle on nav_ansatt.nav_ident = rolle.nav_ansatt_nav_ident
group by nav_ansatt.nav_ident, nav_enhet.enhetsnummer
