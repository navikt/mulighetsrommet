drop view if exists avtale_admin_dto_view;
drop view if exists tiltaksgjennomforing_admin_dto_view;

alter table avtale
    add column leverandor_virksomhet_id uuid references virksomhet (id);
update avtale
set leverandor_virksomhet_id = (select id
                                from virksomhet
                                where organisasjonsnummer = avtale.leverandor_organisasjonsnummer);
create index avtale_leverandor_id_idx on avtale (leverandor_virksomhet_id);
alter table avtale
    alter leverandor_virksomhet_id set not null;
alter table avtale
    drop column leverandor_organisasjonsnummer;

alter table avtale_underleverandor
    add column virksomhet_id uuid references virksomhet (id);
update avtale_underleverandor
set virksomhet_id = (select id
                     from virksomhet
                     where organisasjonsnummer = avtale_underleverandor.organisasjonsnummer);
alter table avtale_underleverandor
    drop constraint avtale_underleverandor_pkey,
    add constraint avtale_underleverandor_pkey primary key (avtale_id, virksomhet_id);
alter table avtale_underleverandor
    alter virksomhet_id set not null;
alter table avtale_underleverandor
    drop column organisasjonsnummer;

alter table tiltaksgjennomforing
    add column arrangor_virksomhet_id uuid references virksomhet (id);
update tiltaksgjennomforing
set arrangor_virksomhet_id = (select id
                              from virksomhet
                              where organisasjonsnummer = tiltaksgjennomforing.arrangor_organisasjonsnummer);
create index tiltaksgjennomforing_arrangor_id_idx on tiltaksgjennomforing (arrangor_virksomhet_id);
alter table tiltaksgjennomforing
    alter arrangor_virksomhet_id set not null;
alter table tiltaksgjennomforing
    drop column arrangor_organisasjonsnummer;

alter table virksomhet_kontaktperson
    add column virksomhet_id uuid references virksomhet (id);
update virksomhet_kontaktperson
set virksomhet_id = (select id
                     from virksomhet
                     where organisasjonsnummer = virksomhet_kontaktperson.organisasjonsnummer);
create index virksomhet_kontaktperson_virksomhet_id_idx on virksomhet_kontaktperson (virksomhet_id);
alter table virksomhet_kontaktperson
    alter virksomhet_id set not null;
alter table virksomhet_kontaktperson
    drop column organisasjonsnummer;
