create type arrangor_rolle as enum ('TILTAK_ARRANGOR_REFUSJON');

create table arrangor_ansatt (
    id          uuid primary key,
    norsk_ident text unique not null,
    fornavn     text,
    etternavn   text
);

CREATE TABLE arrangor_ansatt_rolle
(
    arrangor_ansatt_id  uuid references arrangor_ansatt (id) NOT NULL,
    arrangor_id         uuid references arrangor (id) NOT NULL,
    rolle               arrangor_rolle NOT NULL,
    expiry              timestamp not null,
    primary key (arrangor_id, arrangor_ansatt_id)
);
