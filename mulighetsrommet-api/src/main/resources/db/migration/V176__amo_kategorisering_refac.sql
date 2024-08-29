drop view if exists avtale_admin_dto_view;
drop view if exists tiltaksgjennomforing_admin_dto_view;

create type amo_innhold_element as enum (
    'GRUNNLEGGENDE_FERDIGHETER',
    'TEORETISK_OPPLAERING',
    'JOBBSOKER_KOMPETANSE',
    'PRAKSIS',
    'ARBEIDSMARKEDSKUNNSKAP',
    'NORSKOPPLAERING'
);

create type forerkort_klasse as enum (
    'A', 'A1', 'A2', 'AM', 'AM_147', 'B', 'B_78', 'BE', 'C', 'C1', 'C1E', 'CE', 'D', 'D1', 'D1E', 'DE', 'S', 'T'
);

create type amo_bransje as enum (
    'INGENIOR_OG_IKT_FAG',
    'HELSE_PLEIE_OG_OMSORG',
    'BARNE_OG_UNGDOMSARBEID',
    'KONTORARBEID',
    'BUTIKK_OG_SALGSARBEID',
    'BYGG_OG_ANLEGG',
    'INDUSTRIARBEID',
    'REISELIV_SERVERING_OG_TRANSPORT',
    'SERVICEYRKER_OG_ANNET_ARBEID',
    'ANDRE_BRANSJER'
);

create type amo_kurstype as enum (
    'BRANSJE_OG_YRKESRETTET',
    'NORSKOPPLAERING',
    'GRUNNLEGGENDE_FERDIGHETER',
    'FORBEREDENDE_OPPLAERING_FOR_VOKSNE',
    'STUDIESPESIALISERING'
);

create table amo_sertifisering
(
    konsept_id bigint primary key,
    label text not null
);

create table tiltaksgjennomforing_amo_kategorisering
(
    tiltaksgjennomforing_id uuid primary key references tiltaksgjennomforing (id) on delete cascade,
    kurstype                amo_kurstype not null,
    bransje                 amo_bransje,
    norskprove              boolean,
    innhold_elementer       amo_innhold_element[],
    forerkort               forerkort_klasse[]
);

create table avtale_amo_kategorisering
(
    avtale_id               uuid primary key references avtale (id) on delete cascade,
    kurstype                amo_kurstype not null,
    bransje                 amo_bransje,
    norskprove              boolean,
    innhold_elementer       amo_innhold_element[],
    forerkort               forerkort_klasse[]
);

create table avtale_amo_kategorisering_sertifisering
(
    avtale_id uuid references avtale_amo_kategorisering (avtale_id) on delete cascade,
    konsept_id int references amo_sertifisering (konsept_id) on delete cascade,
    primary key (avtale_id, konsept_id)
);

create table tiltaksgjennomforing_amo_kategorisering_sertifisering
(
    tiltaksgjennomforing_id uuid references tiltaksgjennomforing_amo_kategorisering (tiltaksgjennomforing_id) on delete cascade,
    konsept_id int references amo_sertifisering (konsept_id) on delete cascade,
    primary key (tiltaksgjennomforing_id, konsept_id)
);

alter table avtale drop column amo_kategorisering;
alter table tiltaksgjennomforing drop column amo_kategorisering;
