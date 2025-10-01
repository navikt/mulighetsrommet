create table enkeltplass
(
    id                    uuid                                   not null primary key,
    created_at            timestamp with time zone default now() not null,
    updated_at            timestamp with time zone default now() not null,
    tiltakstype_id        uuid                                   not null references tiltakstype (id),
    arrangor_id           uuid                                   not null references arrangor (id),
    arena_navn            text,
    arena_tiltaksnummer   text unique,
    arena_start_dato      date,
    arena_slutt_dato      date,
    arena_ansvarlig_enhet text,
    arena_status          gjennomforing_status
);

create index enkeltplass__tiltakstype_id_idx on enkeltplass (tiltakstype_id);

create index enkeltplass_arrangor_id_idx on enkeltplass (arrangor_id);

create trigger set_timestamp
    before update
    on enkeltplass
    for each row
execute procedure trigger_set_timestamp();
