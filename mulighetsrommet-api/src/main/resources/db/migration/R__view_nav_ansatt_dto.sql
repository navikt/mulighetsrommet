-- ${flyway:timestamp}

drop view if exists view_nav_ansatt_dto;

create view view_nav_ansatt_dto as
select nav_ansatt.nav_ident,
       nav_ansatt.fornavn,
       nav_ansatt.etternavn,
       nav_ansatt.entra_object_id,
       nav_ansatt.mobilnummer,
       nav_ansatt.epost,
       nav_ansatt.skal_slettes_dato,
       nav_enhet.enhetsnummer as hovedenhet_enhetsnummer,
       nav_enhet.navn         as hovedenhet_navn,
       roller_json
from nav_ansatt
         join nav_enhet on nav_ansatt.hovedenhet = nav_enhet.enhetsnummer
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'rolle',
                                                   rolle.rolle,
                                                   'generell',
                                                   rolle.generell,
                                                   'enheter',
                                                   (select coalesce(jsonb_agg(enhet.nav_enhet_enhetsnummer), '[]'::jsonb)
                                                    from nav_ansatt_rolle_nav_enhet enhet
                                                    where enhet.nav_ansatt_rolle_id = rolle.id)
                                           )
                                   ) as roller_json
                            from nav_ansatt_rolle rolle
                            where nav_ansatt.nav_ident = rolle.nav_ansatt_nav_ident) on true
