alter table tiltakstype rename column tiltakskode to arena_kode;
alter table tiltakstype add column tiltakskode text;

update tiltakstype set tiltakskode = 'AVKLARING' where arena_kode = 'AVKLARAG';
update tiltakstype set tiltakskode = 'OPPFOLGING' where arena_kode = 'INDOPPFAG';
update tiltakstype set tiltakskode = 'DIGITALT_OPPFOLGINGSTILTAK_FOR_ARBEIDSLEDIGE' where arena_kode = 'DIGIOPPARB';
update tiltakstype set tiltakskode = 'GRUPPE_AMO' where arena_kode = 'GRUPPEAMO';
update tiltakstype set tiltakskode = 'JOBBKLUBB' where arena_kode = 'JOBBK';
update tiltakstype set tiltakskode = 'ARBEIDSFORBEREDENDE_TRENING' where arena_kode = 'ARBFORB';
update tiltakstype set tiltakskode = 'FAG_OG_YRKE_OPPLAERING' where arena_kode = 'GRUFAGYRKE';
update tiltakstype set tiltakskode = 'ARBEIDSRETTET_REHABILITERING' where arena_kode = 'ARBRRHDAG';
update tiltakstype set tiltakskode = 'VARIG_TILRETTELAGT_ARBEID' where arena_kode = 'VASV';

alter table tiltakstype_deltaker_registrering_innholdselement
    rename column tiltakskode to arena_kode;
