alter table tiltakstype add column skal_migreres boolean not null default false;

update tiltakstype set skal_migreres = true where tiltakskode in (
    'ARBFORB',
    'ARBRRHDAG',
    'AVKLARAG',
    'GRUPPEAMO',
    'INDOPPFAG',
    'DIGIOPPARB',
    'JOBBK',
    'VASV',
    'GRUFAGYRKE'
);
