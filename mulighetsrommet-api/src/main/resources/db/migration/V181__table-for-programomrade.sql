create table utdanning_programomrade
(
    id       uuid primary key default gen_random_uuid(),
    navn     text not null,
    nus_koder text[],
    programomradekode text unique not null,
    utdanningsprogram utdanning_program
);

alter table utdanning
    drop column programlop_start cascade;

alter table utdanning
    add column programlop_start uuid references utdanning_programomrade(id);

alter table utdanning
    drop constraint utdanning_programomradekode_key;
