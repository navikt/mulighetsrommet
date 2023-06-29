alter table nav_ansatt
    alter column fornavn set data type text collate "nb-NO-x-icu";

alter table nav_ansatt
    alter column etternavn set data type text collate "nb-NO-x-icu";
