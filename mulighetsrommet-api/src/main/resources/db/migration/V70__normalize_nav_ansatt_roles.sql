alter table nav_ansatt
    drop constraint nav_ansatt_pkey cascade;

alter table nav_ansatt
    drop rolle,
    drop fra_ad_gruppe;

alter table nav_ansatt
    add primary key (nav_ident);

alter table nav_ansatt
    add constraint nav_ansatt_azure_id_key unique (azure_id);

alter table nav_ansatt
    add roller nav_ansatt_rolle[] not null default array []::nav_ansatt_rolle[];
