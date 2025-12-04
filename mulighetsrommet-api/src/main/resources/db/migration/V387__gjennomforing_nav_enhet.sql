with gjennomforing_i_tiltadm_uten_nav_enhet as (select g.id as gjennomforing_id, g.arena_ansvarlig_enhet
                                                from gjennomforing g
                                                         left join gjennomforing_nav_enhet gne on g.id = gne.gjennomforing_id
                                                where slutt_dato >= '2023-01-01'
                                                  and gne.enhetsnummer is null),
     arena_nav_enhet as (select gjennomforing_id, overordnet_enhet as enhetsnummer
                         from nav_enhet
                                  join gjennomforing_i_tiltadm_uten_nav_enhet on arena_ansvarlig_enhet = enhetsnummer)
insert
into gjennomforing_nav_enhet(gjennomforing_id, enhetsnummer)
select *
from arena_nav_enhet
where enhetsnummer is not null;
