create type deltaker_forslag_status as enum (
    'GODKJENT',
    'AVVIST',
    'TILBAKEKALT',
    'ERSTATTET',
    'VENTER_PA_SVAR'
);

create table deltaker_forslag (
    id          uuid primary key,
    deltaker_id uuid not null references deltaker(id) on delete cascade,
    endring     jsonb not null,
    status      deltaker_forslag_status not null
)
