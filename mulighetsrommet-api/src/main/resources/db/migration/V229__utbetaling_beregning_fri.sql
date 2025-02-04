create table refusjonskrav_beregning_fri
(
    refusjonskrav_id uuid primary key references refusjonskrav (id),
    periode          daterange not null,
    belop            int     not null
);
