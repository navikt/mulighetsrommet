ALTER TABLE faktura
    ADD COLUMN bic text,
    ADD COLUMN iban text,
    ADD COLUMN bank_land_kode text,
    ADD COLUMN bank_navn text,
    ADD COLUMN valuta_kode text,
    ADD COLUMN betalingskanal text;

