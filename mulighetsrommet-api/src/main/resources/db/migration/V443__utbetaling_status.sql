create table utbetaling_status_type
(
    value text not null primary key
);

insert into utbetaling_status_type(value)
values ('GENERERT'),
       ('INNSENDT'),
       ('TIL_ATTESTERING'),
       ('RETURNERT'),
       ('FERDIG_BEHANDLET'),
       ('DELVIS_UTBETALT'),
       ('UTBETALT'),
       ('AVBRUTT');

alter table utbetaling
    alter status type text;

alter table utbetaling
    add foreign key (status) references utbetaling_status_type (value) on update cascade;

drop type utbetaling_status;

update utbetaling_status_type
set value = 'TIL_BEHANDLING'
where value = 'INNSENDT';
