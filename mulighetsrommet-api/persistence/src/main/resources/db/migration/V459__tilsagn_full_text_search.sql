drop view if exists view_tilsagn;

alter table tilsagn
    add fts tsvector;

update tilsagn t
set fts = to_tsvector('norwegian',
                      concat_ws(' ',
                                t.bestillingsnummer,
                                regexp_replace(t.bestillingsnummer, '/', ' '),
                                to_char(lower(t.periode), 'DD.MM.YYYY'),
                                to_char((upper(t.periode) - interval '1 day')::date, 'DD.MM.YYYY'),
                                t.tilsagn_type
                      )
          );

create index tilsagn_fts_idx on tilsagn using gin (fts);
