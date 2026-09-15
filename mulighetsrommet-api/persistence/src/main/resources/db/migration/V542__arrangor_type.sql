create table arrangor_type
(
    value text not null primary key
);

insert into arrangor_type (value)
values ('NORSK_HOVEDENHET'),
       ('NORSK_UNDERENHET'),
       ('UTENLANDSK');

alter table arrangor
    add column arrangor_type text;

update arrangor
set arrangor_type = case
                        when er_utenlandsk_virksomhet then 'UTENLANDSK'
                        when overordnet_enhet is null then 'NORSK_HOVEDENHET'
                        else 'NORSK_UNDERENHET'
    end;

alter table arrangor
    alter column arrangor_type set not null,
    add foreign key (arrangor_type) references arrangor_type (value) on update cascade,
    drop column er_utenlandsk_virksomhet;
