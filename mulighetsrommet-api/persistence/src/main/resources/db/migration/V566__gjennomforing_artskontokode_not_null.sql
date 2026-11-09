drop view if exists view_gjennomforing;
drop view if exists view_datavarehus_tiltak;

update gjennomforing
set artskontokode = tiltakstype.tiltakskode from tiltakstype
where gjennomforing.tiltakstype_id = tiltakstype.id
  and gjennomforing.artskontokode is null
  and tiltakstype.tiltakskode in (
    'ARBEIDSRETTET_REHABILITERING'
    , 'AVKLARING'
    , 'DIGITALT_OPPFOLGINGSTILTAK'
    , 'JOBBKLUBB'
    , 'OPPFOLGING'
    , 'ARBEIDSFORBEREDENDE_TRENING'
    , 'VARIG_TILRETTELAGT_ARBEID_SKJERMET'
    , 'TILRETTELAGT_ARBEID_ORDINAER'
    , 'ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING'
    , 'ENKELTPLASS_FAG_OG_YRKESOPPLAERING'
    , 'GRUPPE_ARBEIDSMARKEDSOPPLAERING'
    , 'GRUPPE_FAG_OG_YRKESOPPLAERING'
    , 'HOYERE_UTDANNING'
    , 'HOYERE_YRKESFAGLIG_UTDANNING'
    );

alter table gjennomforing
    alter column artskontokode set not null;
