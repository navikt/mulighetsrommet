create table refusjonskrav
(
    id                          uuid primary key,
    tiltaksgjennomforing_id     uuid not null references tiltaksgjennomforing (id),
    created_at                  timestamp default now() not null,
    periode                     date not null,
    arrangor_id                 uuid references arrangor (id),
    beregning                   jsonb not null
);
