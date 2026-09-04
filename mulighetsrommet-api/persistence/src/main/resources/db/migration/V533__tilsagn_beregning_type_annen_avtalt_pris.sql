drop view if exists view_tilsagn;

alter table tilsagn
    alter beregning_type type text;

drop type tilsagn_beregning_type;

create table tilsagn_beregning_type
(
    value text not null primary key
);

insert into tilsagn_beregning_type (value)
values ('FRI'),
       ('PRIS_PER_MANEDSVERK'),
       ('PRIS_PER_UKESVERK'),
       ('PRIS_PER_HELE_UKESVERK'),
       ('FAST_SATS_PER_TILTAKSPLASS_PER_MANED'),
       ('PRIS_PER_TIME_OPPFOLGING');

alter table tilsagn
    add foreign key (beregning_type) references tilsagn_beregning_type (value) on update cascade;

update tilsagn_beregning_type set value = 'ANNEN_AVTALT_PRIS' where value = 'FRI';

alter table tilsagn_fri_beregning
    rename to tilsagn_annen_avtalt_pris_linje;
