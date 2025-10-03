update tiltakstype
set tiltakskode = 'ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING'::tiltakskode
where arena_kode = 'ENKELAMO';

update tiltakstype
set tiltakskode = 'ENKELTPLASS_FAG_OG_YRKESOPPLAERING'::tiltakskode
where arena_kode = 'ENKFAGYRKE';

update tiltakstype
set tiltakskode = 'HOYERE_UTDANNING'::tiltakskode
where arena_kode = 'HOYEREUTD';
