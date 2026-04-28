drop view if exists view_utbetaling_linje;

alter table utbetaling_linje
    alter status type text;

drop type utbetaling_linje_status;

create table utbetaling_linje_status_type
(
    value text not null primary key
);

insert into utbetaling_linje_status_type(value)
values ('TIL_ATTESTERING'),
       ('RETURNERT'),
       ('GODKJENT'),
       ('OVERFORT_TIL_UTBETALING'),
       ('UTBETALT');

alter table utbetaling_linje
    add foreign key (status) references utbetaling_linje_status_type (value) on update cascade;
