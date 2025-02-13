create type besluttelse as enum ('GODKJENT', 'AVVIST');

alter table delutbetaling
    add column besluttet_tidspunkt timestamp,
    add column besluttelse besluttelse,
    add column aarsaker text[],
    add column forklaring text;
