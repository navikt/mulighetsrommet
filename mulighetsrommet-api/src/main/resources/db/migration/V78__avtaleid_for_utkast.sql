alter table utkast
    add column avtale_id uuid references avtale(id) not null;
