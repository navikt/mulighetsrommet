INSERT INTO tiltaksgjennomforing
VALUES (1, 13, 'Truckførerkurs i Namsos',
        'Her kan du lære å kræsje med truck.', 1000, '2020-06-01', '2020-12-01')
ON CONFLICT (id) DO UPDATE SET tittel            = EXCLUDED.tittel,
                               tiltakstype_id = EXCLUDED.tiltakstype_id,
                               beskrivelse       = EXCLUDED.beskrivelse;

SELECT setval('tiltaksgjennomforing_id_seq', (SELECT MAX(id) from "tiltaksgjennomforing"));
