CREATE TABLE arrangor_utenlandsk (
    id uuid PRIMARY KEY not null,
    bic text not null,
    iban text not null,
    bank_navn text not null,
    adresse_gate_navn text not null,
    adresse_by text not null,
    adresse_post_nummer text not null,
    adresse_land_kode text not null,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE arrangor ADD COLUMN arrangor_utenlandsk_id uuid REFERENCES arrangor_utenlandsk(id);
