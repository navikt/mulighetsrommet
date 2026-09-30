insert into prismodell (id, prismodell_type, prisbetingelser, satser, system_id, valuta)
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
       'TILRETTELAGT_ARBEID_ORDINAER',
       'NOK'
where not exists (select 1 from prismodell where system_id = 'TILRETTELAGT_ARBEID_ORDINAER');
