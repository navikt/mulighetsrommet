-- Link mellom vedtak og en revurderingen av det
drop view if exists view_tilskudd_behandling;

create table tilskudd_revurdering (
    tilskudd_id uuid references tilskudd(id) on delete cascade,
    tilskudd_revurdering_id uuid references tilskudd(id) on delete cascade,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null
);

create trigger set_timestamp
    before update
    on tilskudd_revurdering
    for each row
execute procedure trigger_set_timestamp();

create table tilskudd_behandling_type (
    value text primary key
);

insert into tilskudd_behandling_type (value) values ('REGISTRERING'),('REVURDERING');

alter table tilskudd_behandling
    add column type text;

update tilskudd_behandling
set type = 'REGISTRERING'
where type is null;

alter table tilskudd_behandling
    add constraint tilskudd_behandling_type_fkey
        foreign key (type) references tilskudd_behandling_type (value);

alter table tilskudd_behandling
    alter column type set not null;

