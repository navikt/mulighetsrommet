alter table deltaker
    add column registrert_dato timestamp not null default now();
