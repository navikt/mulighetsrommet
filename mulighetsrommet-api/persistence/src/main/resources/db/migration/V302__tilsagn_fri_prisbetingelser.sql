create table tilsagn_fri_prisbetingelser
(
    tilsagn_id      uuid unique not null references tilsagn (id) on delete cascade,
    prisbetingelser text
);

create index on tilsagn_fri_prisbetingelser (tilsagn_id);
