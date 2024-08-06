create type tilsagn_besluttelse as enum ('GODKJENT', 'AVVIST');

alter table tilsagn
    add column besluttelse tilsagn_besluttelse,
    add column besluttet_tidspunkt timestamp,
    drop column sendt_tidspunkt;
