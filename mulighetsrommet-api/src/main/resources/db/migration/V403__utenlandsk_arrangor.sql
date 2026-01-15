CREATE TABLE utenlandsk_arrangor (
    id uuid PRIMARY KEY not null,
    bic text not null,
    iban text not null,
    gate_navn text not null,
    by text not null,
    post_nummer text not null,
    land_kode text not null,
    bank_navn text not null,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE arrangor ADD COLUMN utenlandsk_arrangor_id uuid REFERENCES utenlandsk_arrangor(id);
