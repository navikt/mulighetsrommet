update tiltaksgjennomforing
set publisert = false
where avbrutt_tidspunkt is not null
   or (slutt_dato is not null and slutt_dato < now());
