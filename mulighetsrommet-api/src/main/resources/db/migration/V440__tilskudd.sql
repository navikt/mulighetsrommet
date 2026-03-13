create table tilskudd
(
    id         uuid primary key,
    navn       text                                  not null,
    kode       text                                  not null,
    created_at timestamptz default current_timestamp not null,
    updated_at timestamptz default current_timestamp not null
);

alter table tilskudd
    add constraint tilskudd_navn_unique unique (kode);

create trigger set_timestamp
    before update
    on tilskudd
    for each row
execute procedure trigger_set_timestamp();

insert into tilskudd (id, navn, kode)
values (gen_random_uuid(), 'Skolepenger', 'SKOLEPENGER'),
       (gen_random_uuid(), 'Studiereiser', 'STUDIEREISE'),
       (gen_random_uuid(), 'Eksamensavgift', 'EKSAMENSAVGIFT'),
       (gen_random_uuid(), 'Semesteravgift', 'SEMESTERAVGIFT'),
       (gen_random_uuid(), 'Integrert botilbud', 'INTEGRERT_BOTILBUD');
