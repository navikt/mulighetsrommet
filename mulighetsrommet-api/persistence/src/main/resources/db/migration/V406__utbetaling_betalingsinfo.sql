alter table utbetaling
    add column bic text,
    add column iban text,
    add column bank_land_kode text,
    add column bank_navn text;
