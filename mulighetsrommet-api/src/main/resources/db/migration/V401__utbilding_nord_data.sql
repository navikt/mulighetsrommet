WITH target AS (
    SELECT id
    FROM arrangor
    WHERE organisasjonsnummer = '100000056'
),
ins AS (
    INSERT INTO utenlandsk_arrangor (id, bic, iban, gate_navn, by, post_nummer, land_kode, bank_navn)
    SELECT
        gen_random_uuid(),
        'SWEDSESS',
        'SE7480000826441241851375',
        'Postboks 42',
        'SE-95721 Øvertorneå, Sverige',
        '0000',
        'SE',
        'Sparbanken Nord'
    FROM target
    RETURNING id
)
UPDATE arrangor a
SET utenlandsk_arrangor_id = ins.id
FROM target t, ins
WHERE a.id = t.id;
