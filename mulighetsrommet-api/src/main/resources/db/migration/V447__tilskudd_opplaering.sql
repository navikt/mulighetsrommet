create table tilskudd_opplaering
(
    id         uuid primary key,
    navn       text                                  not null,
    kode       text                                  not null,
    created_at timestamptz default current_timestamp not null,
    updated_at timestamptz default current_timestamp not null
);

alter table tilskudd_opplaering
    add constraint tilskudd_kode_unique unique (kode);

create trigger set_timestamp
    before update
    on tilskudd_opplaering
    for each row
execute procedure trigger_set_timestamp();

insert into tilskudd_opplaering (id, navn, kode)
values (gen_random_uuid(), 'Skolepenger', 'SKOLEPENGER'),
       (gen_random_uuid(), 'Studiereiser', 'STUDIEREISE'),
       (gen_random_uuid(), 'Eksamensavgift', 'EKSAMENSAVGIFT'),
       (gen_random_uuid(), 'Semesteravgift', 'SEMESTERAVGIFT'),
       (gen_random_uuid(), 'Integrert botilbud', 'INTEGRERT_BOTILBUD');
