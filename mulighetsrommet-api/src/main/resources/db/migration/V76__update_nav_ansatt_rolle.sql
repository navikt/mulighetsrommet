alter table nav_ansatt
    alter column roller set data type text[],
    alter column roller drop default;

drop type nav_ansatt_rolle;

create type nav_ansatt_rolle as enum ('TEAM_MULIGHETSROMMET', 'BETABRUKER', 'KONTAKTPERSON');

alter table nav_ansatt
    alter column roller type nav_ansatt_rolle[] using roller::nav_ansatt_rolle[];
