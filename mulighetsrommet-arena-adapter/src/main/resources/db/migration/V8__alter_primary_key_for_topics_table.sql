alter table topics
    drop constraint topics_pkey;

alter table topics
    drop column id;

alter table topics
    rename column key TO id;

alter table topics
    add primary key (id);

alter table topics
    drop constraint topic_unique_key;
