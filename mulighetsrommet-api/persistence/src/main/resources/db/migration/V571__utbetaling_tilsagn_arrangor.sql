create table utbetaling_tilsagn_arrangor
(
    utbetaling_id uuid not null references utbetaling (id) on delete cascade,
    tilsagn_id    uuid not null references tilsagn (id),
    primary key (utbetaling_id, tilsagn_id)
);

create index utbetaling_tilsagn_arrangor_tilsagn_id_idx on utbetaling_tilsagn_arrangor (tilsagn_id);
