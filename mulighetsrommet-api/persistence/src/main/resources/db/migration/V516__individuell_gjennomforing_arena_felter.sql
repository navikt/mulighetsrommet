alter table individuell_gjennomforing
    add column sanity_id     uuid unique,
    add column tiltaksnummer text unique,
    add column start_dato    date,
    add column slutt_dato    date,
    add column status        text check (status in ('GJENNOMFORES', 'AVSLUTTET', 'AVBRUTT', 'AVLYST'));
