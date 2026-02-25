drop view if exists view_utbetaling;

create table utbetaling_blokkering_type(
    value text not null primary key
);

insert into utbetaling_blokkering_type (value) values ('UBEHANDLET_FORSLAG');

create table utbetaling_blokkering (
    utbetaling_id uuid not null references utbetaling(id),
    blokkering text not null references utbetaling_blokkering_type(value),
    primary key (utbetaling_id, blokkering)
);
