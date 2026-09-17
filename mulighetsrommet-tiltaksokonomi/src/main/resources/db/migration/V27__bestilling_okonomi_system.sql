alter table bestilling
    add column okonomi_system text not null default 'TILTAKSADMINISTRASJON';

alter table bestilling
    alter column okonomi_system drop default;
