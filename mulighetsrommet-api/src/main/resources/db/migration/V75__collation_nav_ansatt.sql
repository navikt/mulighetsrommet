alter table nav_ansatt
    alter column fornavn set data type text collate "nb-NO-x-icu";

alter table nav_ansatt
    alter column etternavn set data type text collate "nb-NO-x-icu";

alter table avtale
    add column created_at timestamp default now() not null,
    add column updated_at timestamp default now() not null;
