drop view if exists view_tilsagn;

alter table tilsagn
    add fts tsvector;

update tilsagn t
set fts = to_tsvector('norwegian',
                      concat_ws(' ',
                                t.bestillingsnummer,
                                regexp_replace(t.bestillingsnummer, '/', ' '),
                                to_char(lower(periode), 'DD.MM.YYYY'),
                                to_char((upper(periode) - interval '1 day')::date, 'DD.MM.YYYY'),
                                t.tilsagn_type,
                                ts.navn,
                                g.navn,
                                a.navn,
                                a.organisasjonsnummer
                      )
          )
from gjennomforing g
    join tiltakstype ts on ts.id = g.tiltakstype_id
    join arrangor a on a.id = g.arrangor_id
where
    t.gjennomforing_id = g.id
