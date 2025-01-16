drop view if exists tiltaksgjennomforing_admin_dto_view;

-- rename gjennomforing_id kolonner
alter table del_med_bruker rename column tiltaksgjennomforing_id to gjennomforing_id;
alter table tilsagn rename column tiltaksgjennomforing_id to gjennomforing_id;
alter table tiltaksgjennomforing_administrator rename column tiltaksgjennomforing_id to gjennomforing_id;
alter table tiltaksgjennomforing_amo_kategorisering rename column tiltaksgjennomforing_id to gjennomforing_id;
alter table tiltaksgjennomforing_amo_kategorisering_sertifisering rename column tiltaksgjennomforing_id to gjennomforing_id;
alter table tiltaksgjennomforing_arrangor_kontaktperson rename column tiltaksgjennomforing_id to gjennomforing_id;
alter table tiltaksgjennomforing_kontaktperson rename column tiltaksgjennomforing_id to gjennomforing_id;
alter table tiltaksgjennomforing_nav_enhet rename column tiltaksgjennomforing_id to gjennomforing_id;
alter table tiltaksgjennomforing_utdanningsprogram rename column tiltaksgjennomforing_id to gjennomforing_id;

-- rename amo foreign key og tabeller
alter table tiltaksgjennomforing_amo_kategorisering_sertifisering DROP CONSTRAINT tiltaksgjennomforing_amo_kategori_tiltaksgjennomforing_id_fkey1;
alter table tiltaksgjennomforing_amo_kategorisering RENAME TO gjennomforing_amo_kategorisering;
alter table tiltaksgjennomforing_amo_kategorisering_sertifisering RENAME TO gjennomforing_amo_kategorisering_sertifisering;
alter table gjennomforing_amo_kategorisering_sertifisering add constraint gjennomforing_amo_kategori_gjennomforing_id_fkey1
    foreign key (gjennomforing_id) references gjennomforing_amo_kategorisering (gjennomforing_id) on delete cascade;

-- drop gjennomforing_id foreign keys
alter table del_med_bruker drop constraint del_med_bruker_tiltaksgjennomforing_id_fkey;
alter table deltaker drop constraint deltaker_tiltaksgjennomforing_id_fkey;
alter table tiltaksgjennomforing_administrator drop constraint fk_tiltaksgjennomforing;
alter table tiltaksgjennomforing_nav_enhet drop constraint fk_tiltaksgjennomforing;
alter table tiltaksgjennomforing_kontaktperson drop constraint fk_tiltaksgjennomforing;
alter table refusjonskrav drop constraint refusjonskrav_tiltaksgjennomforing_id_fkey;
alter table tilsagn drop constraint tilsagn_tiltaksgjennomforing_id_fkey;
alter table gjennomforing_amo_kategorisering drop constraint tiltaksgjennomforing_amo_kategoris_tiltaksgjennomforing_id_fkey;
alter table tiltaksgjennomforing_arrangor_kontaktperson drop constraint tiltaksgjennomforing_id_fkey;
alter table tiltaksgjennomforing_utdanningsprogram drop constraint utdanning_programomrade_tiltaksgje_tiltaksgjennomforing_id_fkey;

alter table tiltaksgjennomforing RENAME TO gjennomforing;

-- add gjennomforing_id foreign keys
alter table del_med_bruker add constraint del_med_bruker_gjennomforing_id_fkey
    foreign key (gjennomforing_id) references gjennomforing (id)
    on delete cascade;
alter table deltaker add constraint deltaker_gjennomforing_id_fkey
    foreign key (gjennomforing_id) references gjennomforing (id)
    on delete cascade;
alter table tiltaksgjennomforing_administrator add constraint fk_gjennomforing
    foreign key (gjennomforing_id) references gjennomforing (id)
    on delete cascade;
alter table tiltaksgjennomforing_nav_enhet add constraint fk_gjennomforing
    foreign key (gjennomforing_id) references gjennomforing (id)
    on delete cascade;
alter table tiltaksgjennomforing_kontaktperson add constraint fk_gjennomforing
    foreign key (gjennomforing_id) references gjennomforing (id)
    on delete cascade;
alter table gjennomforing_amo_kategorisering add constraint gjennomforing_amo_kategoris_gjennomforing_id_fkey
    foreign key (gjennomforing_id) references gjennomforing (id)
    on delete cascade;
alter table tiltaksgjennomforing_arrangor_kontaktperson add constraint gjennomforing_id_fkey
    foreign key (gjennomforing_id) references gjennomforing (id)
    on delete cascade;
alter table tiltaksgjennomforing_utdanningsprogram add constraint utdanning_programomrade_gjennomforing_id_fkey
    foreign key (gjennomforing_id) references gjennomforing (id)
    on delete cascade;
alter table refusjonskrav add constraint refusjonskrav_gjennomforing_id_fkey
    foreign key (gjennomforing_id) references gjennomforing (id);
alter table tilsagn add constraint tilsagn_gjennomforing_id_fkey
    foreign key (gjennomforing_id) references gjennomforing (id);

alter table tiltaksgjennomforing_utdanningsprogram RENAME TO gjennomforing_utdanningsprogram;
alter table tiltaksgjennomforing_arrangor_kontaktperson RENAME TO gjennomforing_arrangor_kontaktperson;
alter table tiltaksgjennomforing_kontaktperson RENAME TO gjennomforing_kontaktperson;
alter table tiltaksgjennomforing_nav_enhet RENAME TO gjennomforing_nav_enhet;
alter table tiltaksgjennomforing_administrator RENAME TO gjennomforing_administrator;
alter table tiltaksgjennomforing_endringshistorikk RENAME TO gjennomforing_endringshistorikk;


-- rename enums
alter type tiltaksgjennomforing_oppstartstype rename to gjennomforing_oppstartstype;
alter type arrangor_kontaktperson_ansvarlig_for_type rename value 'TILTAKSGJENNOMFORING' to 'GJENNOMFORING';
alter type filter_dokument_type rename value 'Tiltaksgjennomføring' to 'GJENNOMFORING';
alter type filter_dokument_type rename value 'Tiltaksgjennomføring_Modia' to 'GJENNOMFORING_MODIA';
alter type filter_dokument_type rename value 'Avtale' to 'AVTALE';


