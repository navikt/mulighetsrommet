update gjennomforing
set fts = to_tsvector('norwegian',
                      concat_ws(' ',
                                gjennomforing.lopenummer,
                                regexp_replace(gjennomforing.lopenummer, '/', ' '),
                                coalesce(gjennomforing.arena_tiltaksnummer, ''),
                                gjennomforing.navn,
                                arrangor.navn,
                                tiltakstype.navn
                      )
          )
from arrangor,
     tiltakstype
where arrangor.id = gjennomforing.arrangor_id
  and gjennomforing.tiltakstype_id = tiltakstype.id;
