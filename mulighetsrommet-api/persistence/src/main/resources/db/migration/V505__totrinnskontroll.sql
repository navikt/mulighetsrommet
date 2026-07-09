create table totrinnskontroll_besluttelse_type
(
    value text not null primary key
);

insert into totrinnskontroll_besluttelse_type
values ('TIL_BEHANDLING'),
       ('SATT_PA_VENT'),
       ('AVVIST'),
       ('GODKJENT');

alter table totrinnskontroll
    alter besluttelse type text;

alter table totrinnskontroll
    add foreign key (besluttelse) references totrinnskontroll_besluttelse_type (value) on update cascade;

drop type besluttelse;

update totrinnskontroll
set besluttelse = 'SATT_PA_VENT'
where type = 'ENKELTPLASS_OKONOMI'
  and besluttelse = 'AVVIST';
