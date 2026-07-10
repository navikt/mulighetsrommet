drop view if exists avtale_admin_dto_view;
drop view if exists tiltaksgjennomforing_admin_dto_view;

alter table virksomhet
    add constraint virksomhet_organisasjonsnummer_idx unique (organisasjonsnummer),
    drop constraint virksomhet_overordnet_enhet_fkey,
    drop constraint virksomhet_pkey,
    add constraint virksomhet_pkey primary key (id),
    add constraint virksomhet_overordnet_enhet_fkey foreign key (organisasjonsnummer) references virksomhet (organisasjonsnummer) on delete cascade;
