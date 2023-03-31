alter table enhet
    add column type text;

update enhet
    set type = 'LOKAL';

alter table enhet
    alter column type set not null;

