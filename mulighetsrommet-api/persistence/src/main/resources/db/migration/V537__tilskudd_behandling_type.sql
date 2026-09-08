drop view if exists view_tilskudd_behandling;

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
        foreign key (type) references tilskudd_behandling_type (value),
    alter column type set not null;
