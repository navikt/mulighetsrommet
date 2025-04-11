alter table nav_ansatt_rolle
    add generell boolean;

update nav_ansatt_rolle
set generell = true;

alter table nav_ansatt_rolle
    alter generell set not null;
