alter table totrinnskontroll
    alter type type text;

drop type totrinnskontroll_type;

create table totrinnskontroll_type
(
    value text not null primary key
);

insert into totrinnskontroll_type
values ('OPPRETT'),
       ('ANNULLER'),
       ('GJOR_OPP'),
       ('OKONOMI');

alter table totrinnskontroll
    add foreign key (type) references totrinnskontroll_type (value) on update cascade;
