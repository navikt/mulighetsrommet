create or replace function trigger_set_timestamp()
    returns trigger as
$$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create table bestilling
(
    id                  int generated always as identity,
    created_at          timestamptz default now(),
    updated_at          timestamptz default now(),
    bestillingsnummer   text unique not null,
    avtalenummer        text,
    tiltakskode         text        not null,
    arrangor_hovedenhet text        not null,
    arrangor_underenhet text        not null,
    kostnadssted        text        not null,
    belop               int         not null,
    periode             daterange   not null,
    status              text        not null,
    opprettet_av        text        not null,
    opprettet_tidspunkt timestamptz not null,
    besluttet_av        text        not null,
    besluttet_tidspunkt timestamptz not null
);

create trigger set_timestamp
    before update
    on bestilling
    for each row
execute procedure trigger_set_timestamp();

create table bestilling_linje
(
    id            int generated always as identity,
    created_at    timestamptz default now(),
    bestilling_id int,
    linjenummer   int       not null,
    periode       daterange not null,
    belop         int       not null,
    unique (bestilling_id, linjenummer)
);

create table faktura
(
    id                  int generated always as identity,
    created_at          timestamptz default now(),
    updated_at          timestamptz default now(),
    fakturanummer       text unique not null,
    bestillingsnummer   text        not null references bestilling (bestillingsnummer),
    kontonummer         text        not null,
    kid                 text,
    belop               int         not null,
    periode             daterange   not null,
    status              text        not null,
    opprettet_av        text        not null,
    opprettet_tidspunkt timestamptz not null,
    besluttet_av        text        not null,
    besluttet_tidspunkt timestamptz not null
);

create trigger set_timestamp
    before update
    on faktura
    for each row
execute procedure trigger_set_timestamp();

create table faktura_linje
(
    id          int generated always as identity,
    created_at  timestamptz default now(),
    faktura_id  int       not null,
    linjenummer int       not null,
    periode     daterange not null,
    belop       int       not null,
    unique (faktura_id, linjenummer)
);
