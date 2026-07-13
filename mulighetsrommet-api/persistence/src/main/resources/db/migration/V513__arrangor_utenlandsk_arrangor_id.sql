alter table arrangor_utenlandsk rename column id to arrangor_id;

alter table arrangor_utenlandsk
    add constraint arrangor_utenlandsk_arrangor_id_fkey
        foreign key (arrangor_id) references arrangor (id) on delete cascade;

alter table arrangor drop column arrangor_utenlandsk_id;
