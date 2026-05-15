alter table prismodell
    alter prismodell_type type text;

drop type prismodell_type;

create table prismodell_type
(
    value text not null primary key
);

insert into prismodell_type (value)
values ('FORHANDSGODKJENT_PRIS_PER_MANEDSVERK'),
       ('FORHANDSGODKJENT_PRIS_PER_AVTALT_TILTAKSPLASS'),
       ('AVTALT_PRIS_PER_MANEDSVERK'),
       ('AVTALT_PRIS_PER_UKESVERK'),
       ('AVTALT_PRIS_PER_HELE_UKESVERK'),
       ('ANNEN_AVTALT_PRIS'),
       ('AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER'),
       ('TILSKUDD_TIL_OPPLAERING'),
       ('INGEN_KOSTNADER');

alter table prismodell
    add foreign key (prismodell_type) references prismodell_type (value) on update cascade;

update prismodell_type set value = 'FAST_SATS_PER_BENYTTET_PLASS_PER_MANED' where value = 'FORHANDSGODKJENT_PRIS_PER_MANEDSVERK';
update prismodell_type set value = 'FAST_SATS_PER_AVTALT_PLASS_PER_MANED' where value = 'FORHANDSGODKJENT_PRIS_PER_AVTALT_TILTAKSPLASS';
update prismodell_type set value = 'AVTALT_PRIS_PER_BENYTTET_PLASS_PER_MANED' where value = 'AVTALT_PRIS_PER_MANEDSVERK';
update prismodell_type set value = 'AVTALT_PRIS_PER_BENYTTET_PLASS_PER_UKE' where value = 'AVTALT_PRIS_PER_UKESVERK';
update prismodell_type set value = 'AVTALT_PRIS_PER_BENYTTET_PLASS_PER_HELE_UKE' where value = 'AVTALT_PRIS_PER_HELE_UKESVERK';
