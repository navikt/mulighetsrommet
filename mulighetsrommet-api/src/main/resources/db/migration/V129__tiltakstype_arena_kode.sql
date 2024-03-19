drop view if exists avtale_admin_dto_view;
drop view if exists tiltaksgjennomforing_admin_dto_view;

alter table tiltakstype rename column tiltakskode to arena_kode;
alter table tiltakstype drop column skal_migreres;

create type tiltakskode as enum (
    'AVKLARING',
    'OPPFOLGING',
    'DIGITALT_OPPFOLGINGSTILTAK',
    'GRUPPE_ARBEIDSMARKEDSOPPLAERING',
    'JOBBKLUBB',
    'ARBEIDSFORBEREDENDE_TRENING',
    'FAG_OG_YRKESOPPLAERING',
    'ARBEIDSRETTET_REHABILITERING',
    'VARIG_TILRETTELAGT_ARBEID_SKJERMET'
);

alter table tiltakstype add column tiltakskode tiltakskode;

update tiltakstype set tiltakskode = 'AVKLARING' where arena_kode = 'AVKLARAG';
update tiltakstype set tiltakskode = 'OPPFOLGING' where arena_kode = 'INDOPPFAG';
update tiltakstype set tiltakskode = 'DIGITALT_OPPFOLGINGSTILTAK' where arena_kode = 'DIGIOPPARB';
update tiltakstype set tiltakskode = 'GRUPPE_ARBEIDSMARKEDSOPPLAERING' where arena_kode = 'GRUPPEAMO';
update tiltakstype set tiltakskode = 'JOBBKLUBB' where arena_kode = 'JOBBK';
update tiltakstype set tiltakskode = 'ARBEIDSFORBEREDENDE_TRENING' where arena_kode = 'ARBFORB';
update tiltakstype set tiltakskode = 'FAG_OG_YRKESOPPLAERING' where arena_kode = 'GRUFAGYRKE';
update tiltakstype set tiltakskode = 'ARBEIDSRETTET_REHABILITERING' where arena_kode = 'ARBRRHDAG';
update tiltakstype set tiltakskode = 'VARIG_TILRETTELAGT_ARBEID_SKJERMET' where arena_kode = 'VASV';

alter table tiltakstype_deltaker_registrering_innholdselement rename column tiltakskode to arena_kode;
