create table refusjonskrav_beregning_fri
(
    refusjonskrav_id uuid primary key references refusjonskrav (id),
    belop            int     not null
);
