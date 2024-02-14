alter table virksomhet
    add column updated_at timestamp default now() not null;
