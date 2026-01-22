-- Nytt case hvor gjennomføring har blitt avbrutt noen dager etter den egentlig skulle vært
-- avbrutt og derfor har noen deltakere for sen sluttdato
update utbetaling
    set status = 'AVBRUTT'
    where id = '92d657f4-719c-4cae-b2ac-8454785099e2'
    and gjennomforing_id = 'f6aa28b0-7e8d-4c73-b58a-303e09a70c10'
