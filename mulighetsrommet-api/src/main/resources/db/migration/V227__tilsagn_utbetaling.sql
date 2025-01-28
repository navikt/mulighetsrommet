create table tilsagn_utbetaling
(
    refusjonskrav_id uuid not null references refusjonskrav(id),
    tilsagn_id       uuid not null references tilsagn(id),
    belop            int not null,
    created_at       timestamp not null default now(),
    opprettet_av     text not null,
    besluttet_av     text,
    primary key (refusjonskrav_id, tilsagn_id)
);
