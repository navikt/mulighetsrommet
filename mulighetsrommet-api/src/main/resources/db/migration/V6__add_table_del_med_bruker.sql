create table del_med_bruker
(
    id            serial                  not null primary key,
    bruker_fnr    text                    not null,
    navident      text                    not null,
    tiltaksnummer text                    not null,
    created_at    timestamp default now() not null,
    updated_at    timestamp default now() not null,
    created_by    text,
    updated_by    text
);

create index del_med_bruker_oppslag
    on del_med_bruker (bruker_fnr, navident, tiltaksnummer);
