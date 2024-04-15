create type arrangor_kontaktperson_ansvarlig_for_type as enum ('AVTALE', 'TILTAKSGJENNOMFORING', 'OKONOMI');

alter table arrangor_kontaktperson
    add column ansvarlig_for arrangor_kontaktperson_ansvarlig_for_type[];
