alter table bestilling
    add column fagsystem text not null default 'TILTAKSADMINISTRASJON';

alter table bestilling
    alter column fagsystem drop default;
