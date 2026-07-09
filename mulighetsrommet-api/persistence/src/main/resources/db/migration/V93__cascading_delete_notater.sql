alter table avtale_notat
    drop constraint avtale_notat_avtale_id_fkey;

alter table avtale_notat
    add constraint avtale_notat_avtale_id_fkey foreign key (avtale_id) references avtale (id) on delete cascade;

alter table avtale_notat
    drop constraint avtale_notat_opprettet_av_fkey;

alter table avtale_notat
    add constraint avtale_notat_opprettet_av_fkey foreign key (opprettet_av) references nav_ansatt (nav_ident) on delete cascade;

alter table tiltaksgjennomforing_notat
    drop constraint tiltaksgjennomforing_notat_tiltaksgjennomforing_id_fkey;

alter table tiltaksgjennomforing_notat
    add constraint tiltaksgjennomforing_notat_tiltaksgjennomforing_id_fkey foreign key (tiltaksgjennomforing_id) references tiltaksgjennomforing (id) on delete cascade;

alter table tiltaksgjennomforing_notat
    drop constraint tiltaksgjennomforing_notat_opprettet_av_fkey;

alter table tiltaksgjennomforing_notat
    add constraint tiltaksgjennomforing_notat_opprettet_av_fkey foreign key (opprettet_av) references nav_ansatt (nav_ident) on delete cascade;
