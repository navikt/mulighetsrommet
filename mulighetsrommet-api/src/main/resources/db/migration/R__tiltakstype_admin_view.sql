drop view if exists tiltakstype_admin_dto_view;

create view tiltakstype_admin_dto_view as
select id,
       navn,
       tiltakskode,
       arena_kode,
       registrert_dato_i_arena,
       sist_endret_dato_i_arena,
       fra_dato,
       til_dato,
       sanity_id,
       rett_paa_tiltakspenger,
       case
           when now() > til_dato then 'Avsluttet'
           when now() >= fra_dato then 'Aktiv'
           else 'Planlagt'
           end as status
from tiltakstype
