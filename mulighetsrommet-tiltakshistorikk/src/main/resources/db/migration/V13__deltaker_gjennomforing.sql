alter table komet_deltaker
    drop constraint komet_deltaker_gjennomforing_id_fkey;

alter table komet_deltaker
    add constraint fk_komet_deltaker_gjennomforing
        foreign key (gjennomforing_id) references gjennomforing (id);
