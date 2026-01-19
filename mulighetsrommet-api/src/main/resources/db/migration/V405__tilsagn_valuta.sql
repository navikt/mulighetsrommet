create type currency as enum ('NOK');

alter table tilsagn
    add column valuta currency not null default 'NOK';
update tilsagn
set valuta = 'NOK'::currency;

alter table tilsagn_fri_beregning
    add column valuta currency not null default 'NOK';
update tilsagn_fri_beregning
set valuta = 'NOK'::currency;
