create table tiltaksgjennomforing_virksomhet_kontaktperson
(
    virksomhet_kontaktperson_id uuid not null references virksomhet_kontaktperson (id) on delete cascade,
    tiltaksgjennomforing_id     uuid not null references tiltaksgjennomforing (id) on delete cascade,
    primary key (virksomhet_kontaktperson_id, tiltaksgjennomforing_id)
);

insert into tiltaksgjennomforing_virksomhet_kontaktperson (virksomhet_kontaktperson_id, tiltaksgjennomforing_id)
select arrangor_kontaktperson_id, id from tiltaksgjennomforing where arrangor_kontaktperson_id is not null;

drop view if exists tiltaksgjennomforing_admin_dto_view;
alter table tiltaksgjennomforing drop column arrangor_kontaktperson_id;
