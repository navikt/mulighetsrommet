create or replace function trigger_set_timestamp()
    returns trigger as
$$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create table tilsagn
(
    id             int generated always as identity,
    tilsagnsnummer text unique not null,
    tiltakskode    text        not null,
    deltaker_id    int         not null,
    created_at     timestamptz default now(),
    updated_at     timestamptz default now()
);

create trigger set_timestamp
    before update
    on tilsagn
    for each row
execute procedure trigger_set_timestamp();

create table oebs_bestilling_status
(
    id             int generated always as identity,
    tilsagnsnummer text not null references tilsagn (tilsagnsnummer),
    created_at     timestamptz default now(),
    updated_at     timestamptz default now(),
    status         text not null
);

create table oebs_faktura
(
    id text primary key not null
);
