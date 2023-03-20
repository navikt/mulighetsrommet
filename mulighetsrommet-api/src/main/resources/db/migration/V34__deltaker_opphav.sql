create type deltakeropphav as enum ('ARENA', 'AMT');

alter table deltaker
    add column opphav deltakeropphav not null default 'ARENA';

alter table deltaker
    alter column opphav drop default;
