alter table endringshistorikk
    alter document_class type text;

drop type document_class;

create table endringshistorikk_type
(
    value text not null primary key
);

insert into endringshistorikk_type(value)
values ('TILTAKSTYPE'),
       ('AVTALE'),
       ('GJENNOMFORING'),
       ('TILSAGN'),
       ('UTBETALING'),
       ('TILSKUDD_BEHANDLING');

alter table endringshistorikk
    add foreign key (document_class) references endringshistorikk_type (value) on update cascade;

insert into nav_ansatt_rolle_type
values ('TILTAKSTYPER_REDIGER_DELTAKERINFO');
