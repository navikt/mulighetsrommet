create type currency_type as enum ('NOK');

alter table tilsagn
    add column valuta currency_type not null default 'NOK';
update tilsagn
set valuta = 'NOK'::currency_type;

alter table tilsagn_fri_beregning
    add column valuta currency_type not null default 'NOK';
update tilsagn_fri_beregning
set valuta = 'NOK'::currency_type;
