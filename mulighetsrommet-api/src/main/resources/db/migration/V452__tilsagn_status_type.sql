drop view if exists view_tilsagn;

alter table tilsagn
    alter status type text;

drop type tilsagn_status;

create table tilsagn_status_type
(
    value text not null primary key
);

insert into tilsagn_status_type(value)
values ('TIL_GODKJENNING'),
       ('GODKJENT'),
       ('RETURNERT'),
       ('TIL_ANNULLERING'),
       ('ANNULLERT'),
       ('TIL_OPPGJOR'),
       ('OPPGJORT');

alter table tilsagn
    add foreign key (status) references tilsagn_status_type (value) on update cascade;

alter table tilsagn
    alter tilsagn_type type text;

drop type tilsagn_type;

create table tilsagn_type
(
    value text not null primary key
);

insert into tilsagn_type(value)
values ('TILSAGN'),
       ('EKSTRATILSAGN'),
       ('INVESTERING');

alter table tilsagn
    add foreign key (tilsagn_type) references tilsagn_type (value) on update cascade;
