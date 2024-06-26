alter table komet_deltaker
    add foreign key (gjennomforing_id) references gruppetiltak (id);
