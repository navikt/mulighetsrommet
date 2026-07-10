drop view if exists avtale_admin_dto_view;

create table avtale_arrangor_kontaktperson
(
    arrangor_kontaktperson_id uuid not null references arrangor_kontaktperson (id) on delete cascade,
    avtale_id                 uuid not null references avtale (id) on delete cascade,
    primary key (arrangor_kontaktperson_id, avtale_id)
);

insert into avtale_arrangor_kontaktperson (arrangor_kontaktperson_id, avtale_id)
select arrangor_kontaktperson_id, id from avtale where arrangor_kontaktperson_id is not null;

alter table avtale drop column arrangor_kontaktperson_id;
