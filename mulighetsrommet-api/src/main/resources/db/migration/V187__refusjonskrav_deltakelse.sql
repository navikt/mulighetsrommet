alter table refusjonskrav
    drop column beregning;

create table refusjonskrav_beregning_aft
(
    refusjonskrav_id uuid primary key references refusjonskrav (id),
    belop            int not null,
    sats             int not null
);

create table refusjonskrav_deltakelse_periode
(
    refusjonskrav_id uuid references refusjonskrav (id) not null,
    deltakelse_id    uuid                               not null,
    periode          tsrange                            not null,
    prosent_stilling numeric(5, 2)                      not null,
    exclude using gist(deltakelse_id with =, periode with &&)
);

create index refusjonskrav_deltakelse_periode_refusjonskrav_idx on refusjonskrav_deltakelse_periode (refusjonskrav_id);

create index refusjonskrav_deltakelse_periode_deltakelse_idx on refusjonskrav_deltakelse_periode (deltakelse_id);
