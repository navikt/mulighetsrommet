update tilsagn t
set
    beregning_type = 'FAST_SATS_PER_TILTAKSPLASS_PER_MANED'
from gjennomforing g
inner join avtale a on g.avtale_id = a.id
where
    g.id = t.gjennomforing_id
  and  a.prismodell = 'FORHANDSGODKJENT_PRIS_PER_MANEDSVERK'
  and t.beregning_type = 'PRIS_PER_MANEDSVERK'
  and a.id = 'e478ce1f-1ab9-466d-9314-50bb3f3be745';
