update gjennomforing set slutt_dato = start_dato
    where slutt_dato < start_dato
    and status = 'AVBRUTT'

