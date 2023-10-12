create type innsatsgruppe as enum (
    'STANDARD_INNSATS',
    'SITUASJONSBESTEMT_INNSATS',
    'SPESIELT_TILPASSET_INNSATS',
    'VARIG_TILPASSET_INNSATS'
    );

alter table tiltakstype add column innsatsgruppe innsatsgruppe;
