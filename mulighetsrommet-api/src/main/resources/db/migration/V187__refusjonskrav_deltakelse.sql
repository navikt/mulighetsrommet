create extension if not exists btree_gist;

drop view if exists refusjonskrav_aft_view;
drop view if exists refusjonskrav_admin_dto_view;

alter table refusjonskrav
    drop column beregning,
    drop column periode_start,
    drop column periode_slutt;

alter table refusjonskrav
    rename column tiltaksgjennomforing_id to gjennomforing_id;

create table refusjonskrav_beregning_aft
(
    refusjonskrav_id uuid primary key references refusjonskrav (id),
    periode          tsrange not null,
    sats             int     not null,
    belop            int     not null
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

create table refusjonskrav_deltakelse_manedsverk
(
    refusjonskrav_id uuid references refusjonskrav (id) not null,
    deltakelse_id    uuid                               not null,
    manedsverk       numeric(5, 2)                      not null,
    unique (refusjonskrav_id, deltakelse_id)
);

create index refusjonskrav_deltakelse_manedsverk_refusjonskrav_idx on refusjonskrav_deltakelse_manedsverk (refusjonskrav_id);

create index refusjonskrav_deltakelse_manedsverk_deltakelse_idx on refusjonskrav_deltakelse_manedsverk (deltakelse_id);
