insert into prismodell (id, prismodell_type, prisbetingelser, satser, system_id)
select gen_random_uuid(),
       'FAST_SATS_PER_AVTALT_PLASS_PER_MANED',
       null,
       '[
         {
           "gjelderFra": "2026-01-01",
           "sats": 7321,
           "valuta": "NOK"
         }
       ]',
       'TILRETTELAGT_ARBEID_ORDINAER'
where not exists (select 1 from prismodell where system_id = 'TILRETTELAGT_ARBEID_ORDINAER');

update avtale_prismodell
set prismodell_id = (select id from prismodell where system_id = 'TILRETTELAGT_ARBEID_ORDINAER')
where avtale_id in (select avtale.id
                    from avtale
                             join tiltakstype on avtale.tiltakstype_id = tiltakstype.id
                    where tiltakskode = 'TILRETTELAGT_ARBEID_ORDINAER');

update gjennomforing
set prismodell_id = (select id from prismodell where system_id = 'TILRETTELAGT_ARBEID_ORDINAER')
where id in (select gjennomforing.id
             from gjennomforing
                      join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
             where tiltakskode = 'TILRETTELAGT_ARBEID_ORDINAER');
