alter table nav_ansatt
    add skal_slettes_dato timestamp;

alter table user_notification
    add constraint fk_nav_ansatt foreign key (user_id) references nav_ansatt (nav_ident) on delete cascade;

alter table tiltaksgjennomforing_ansvarlig
    add constraint fk_nav_ansatt foreign key (navident) references nav_ansatt (nav_ident) on delete cascade;

alter table avtale_ansvarlig
    add constraint fk_nav_ansatt foreign key (navident) references nav_ansatt (nav_ident) on delete cascade;
