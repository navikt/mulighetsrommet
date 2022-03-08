INSERT INTO tiltaksgjennomforing (id, tittel, beskrivelse, tiltaksnummer, fra_dato, til_dato, tiltakskode)
VALUES (1, 'Truckførerkurs i Namsos',
        'Her kan du lære å kræsje med truck.', 1000, '2020-06-01', '2020-12-01', 'ABIST')
ON CONFLICT (id) DO UPDATE SET tittel            = EXCLUDED.tittel,
                               beskrivelse       = EXCLUDED.beskrivelse;

SELECT setval('tiltaksgjennomforing_id_seq', (SELECT MAX(id) from "tiltaksgjennomforing"));
