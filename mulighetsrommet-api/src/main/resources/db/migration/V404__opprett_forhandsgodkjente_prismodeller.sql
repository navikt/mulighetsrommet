do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'cloudsqliamuser') then
            insert into prismodell (id, prismodell_type, prisbetingelser, satser, system_id)
            values (gen_random_uuid(),
                    'FORHANDSGODKJENT_PRIS_PER_MANEDSVERK',
                    null,
                    '[
                      {
                        "gjelderFra": "2025-01-01",
                        "sats": 20975,
                        "valuta": "NOK"
                      },
                      {
                        "gjelderFra": "2026-01-01",
                        "sats": 21730,
                        "valuta": "NOK"
                      }
                    ]',
                    'ARBEIDSFORBEREDENDE_TRENING'),
                   (gen_random_uuid(),
                    'FORHANDSGODKJENT_PRIS_PER_MANEDSVERK',
                    null,
                    '[
                      {
                        "gjelderFra": "2025-01-01",
                        "sats": 16848,
                        "valuta": "NOK"
                      },
                      {
                        "gjelderFra": "2026-01-01",
                        "sats": 17455,
                        "valuta": "NOK"
                      }
                    ]',
                    'VARIG_TILRETTELAGT_ARBEID_SKJERMET');
        end if;
    end
$$;

update avtale_prismodell
set prismodell_id = (select id from prismodell where system_id = 'ARBEIDSFORBEREDENDE_TRENING')
where avtale_id in (select avtale.id
                    from avtale
                             join tiltakstype on avtale.tiltakstype_id = tiltakstype.id
                    where tiltakskode = 'ARBEIDSFORBEREDENDE_TRENING');

update gjennomforing
set prismodell_id = (select id from prismodell where system_id = 'ARBEIDSFORBEREDENDE_TRENING')
where id in (select gjennomforing.id
             from gjennomforing
                      join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
             where tiltakskode = 'ARBEIDSFORBEREDENDE_TRENING');

update avtale_prismodell
set prismodell_id = (select id from prismodell where system_id = 'VARIG_TILRETTELAGT_ARBEID_SKJERMET')
where avtale_id in (select avtale.id
                    from avtale
                             join tiltakstype on avtale.tiltakstype_id = tiltakstype.id
                    where tiltakskode = 'VARIG_TILRETTELAGT_ARBEID_SKJERMET');

update gjennomforing
set prismodell_id = (select id from prismodell where system_id = 'VARIG_TILRETTELAGT_ARBEID_SKJERMET')
where id in (select gjennomforing.id
             from gjennomforing
                      join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
             where tiltakskode = 'VARIG_TILRETTELAGT_ARBEID_SKJERMET');

delete
from prismodell
where prismodell_type = 'FORHANDSGODKJENT_PRIS_PER_MANEDSVERK'
  and system_id is null;
