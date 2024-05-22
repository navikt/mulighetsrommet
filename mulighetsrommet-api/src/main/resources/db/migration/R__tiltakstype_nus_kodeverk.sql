/**
 * Script for å koble nus_kodeverk med tiltakstyper
 */

insert into tiltakstype_nus_kodeverk(tiltakskode, code, version)
select *
from (values
          ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3', '2437'),
          ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '30', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '31', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '32', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '33',
              '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '34',
              '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '35',
              '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '36',
              '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '37',
              '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '38', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3551', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3552', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '357', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '3571', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '4', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '40', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '41', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '42', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '43',
              '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '44',
              '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '45',
              '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '46',
              '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '47',
              '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '48', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '4551', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '4552', '2437'),
             ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, '4571', '2437')
      ) as t(tiltakskode, code, version)
-- Sjekker om tiltakstypen eksisterer så ikke testene brekker når vi kjører de
where exists(select 1 from tiltakstype where tiltakskode = 'GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode)
on conflict (tiltakskode, code, version) do nothing;


