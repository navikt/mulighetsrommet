alter table avtale_ansvarlig
    rename to avtale_administrator;

alter table avtale_administrator
    rename navident to nav_ident;

alter table tiltaksgjennomforing_ansvarlig
    rename to tiltaksgjennomforing_administrator;

alter table tiltaksgjennomforing_administrator
    rename navident to nav_ident;
