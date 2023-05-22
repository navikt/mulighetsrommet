create table virksomhet
(
    organisasjonsnummer     text not null,
    navn                    text not null,
    created_at              timestamp default now() not null,
    updated_at              timestamp default now() not null,
    primary key (organisasjonsnummer)
);

create table underenhet
(
    underenhetsnummer       text not null,
    organisasjonsnummer     text not null constraint fk_virksomhet references virksomhet (organisasjonsnummer) on delete cascade,
    navn                    text not null,
    created_at              timestamp default now() not null,
    updated_at              timestamp default now() not null,
    primary key (underenhetsnummer)
);
