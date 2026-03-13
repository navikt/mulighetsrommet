create table tilskudd
(
    id         uuid primary key,
    navn       text                                  not null,
    created_at timestamptz default current_timestamp not null,
    updated_at timestamptz default current_timestamp not null
);

alter table tilskudd
    add constraint tilskudd_navn_unique unique (navn);

create trigger set_timestamp
    before update
    on tilskudd
    for each row
execute procedure trigger_set_timestamp();

insert into tilskudd (id, navn)
values (gen_random_uuid(), 'SKOLEPENGER'),
       (gen_random_uuid(), 'STUDIEREISE'),
       (gen_random_uuid(), 'EKSAMENSAVGIFT'),
       (gen_random_uuid(), 'SEMESTERAVGIFT'),
       (gen_random_uuid(), 'INTEGRERT_BOTILBUD');
