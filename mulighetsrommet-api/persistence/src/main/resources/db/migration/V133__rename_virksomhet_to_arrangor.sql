drop view if exists avtale_admin_dto_view;
drop view if exists tiltaksgjennomforing_admin_dto_view;

alter table virksomhet
    rename to arrangor;

alter table virksomhet_kontaktperson
    rename to arrangor_kontaktperson;

alter table avtale_underleverandor
    rename to avtale_arrangor_underenhet;

alter table tiltaksgjennomforing_virksomhet_kontaktperson
    rename to tiltaksgjennomforing_arrangor_kontaktperson;

alter table arrangor
    rename constraint virksomhet_id_key to arrangor_id_key;
alter table arrangor
    rename constraint virksomhet_pkey to arrangor_pkey;
alter table arrangor
    rename constraint virksomhet_organisasjonsnummer_idx to arrangor_organisasjonsnummer_idx;
alter table arrangor
    rename constraint virksomhet_overordnet_enhet_fkey to arrangor_overordnet_enhet_fkey;
drop index virksomhet_overordnet_enhet_idx;
create index arrangor_overordnet_enhet_idx on arrangor (organisasjonsnummer);

alter table arrangor_kontaktperson
    rename column virksomhet_id to arrangor_id;
alter table arrangor_kontaktperson
    rename constraint virksomhet_kontaktperson_pkey to arrangor_kontaktperson_pkey;
alter table arrangor_kontaktperson
    rename constraint virksomhet_kontaktperson_virksomhet_id_fkey to arrangor_id_fkey;

alter table avtale
    rename column leverandor_virksomhet_id to arrangor_hovedenhet_id;
alter table avtale
    rename column leverandor_kontaktperson_id to arrangor_kontaktperson_id;
alter table avtale
    rename constraint avtale_leverandor_virksomhet_id_fkey to avtale_arrangor_id_fkey;

alter table avtale_arrangor_underenhet
    rename column virksomhet_id to arrangor_id;
alter table avtale_arrangor_underenhet
    rename constraint avtale_underleverandor_virksomhet_id_fkey to avtale_arrangor_underenhet_id_fkey;

alter table tiltaksgjennomforing
    rename column arrangor_virksomhet_id to arrangor_id;
alter table tiltaksgjennomforing
    rename constraint tiltaksgjennomforing_arrangor_virksomhet_id_fkey to tiltaksgjennomforing_arrangor_id_fkey;

alter table tiltaksgjennomforing_arrangor_kontaktperson
    rename column virksomhet_kontaktperson_id to arrangor_kontaktperson_id;
alter table tiltaksgjennomforing_arrangor_kontaktperson
    rename constraint tiltaksgjennomforing_virksomhe_virksomhet_kontaktperson_id_fkey to kontaktperson_id_fkey;
alter table tiltaksgjennomforing_arrangor_kontaktperson
    rename constraint tiltaksgjennomforing_virksomhet_ko_tiltaksgjennomforing_id_fkey to tiltaksgjennomforing_id_fkey;
alter table tiltaksgjennomforing_arrangor_kontaktperson
    rename constraint tiltaksgjennomforing_virksomhet_kontaktperson_pkey to tiltaksgjennomforing_arrangor_kontaktperson_pkey;

alter index virksomhet_kontaktperson_virksomhet_id_idx rename to arrangor_kontaktperson_arrangor_id_idx;
