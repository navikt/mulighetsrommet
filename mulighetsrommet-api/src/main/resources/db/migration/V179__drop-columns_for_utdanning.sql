drop type if exists utdanningstype cascade;
drop type if exists studieretning cascade;

drop table if exists utdanning cascade;

create type utdanning_program as enum ('YRKESFAGLIG', 'STUDIEFORBEREDENDE');
create type utdanning_sluttkompetanse as enum ('Fagbrev', 'Svennebrev', 'Studiekompetanse', 'Yrkeskompetanse');
create type utdanning_status as enum ('GYLDIG', 'KOMMENDE', 'UTGAAENDE', 'UTGAATT');

create table utdanning
(
    id                varchar(255) primary key,
    navn              varchar(255)            not null,
    utdanningsprogram utdanning_program,
    programlop_start  varchar(255)            not null,
    sluttkompetanse   utdanning_sluttkompetanse,
    aktiv             boolean                 not null,
    utdanningstatus   utdanning_status        not null,
    utdanningslop     text[],
    created_at        timestamp default now() not null,
    updated_at        timestamp default now() not null
);
