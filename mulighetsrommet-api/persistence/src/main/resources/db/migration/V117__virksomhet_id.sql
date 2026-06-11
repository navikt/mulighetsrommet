alter table virksomhet
    add column id           uuid unique default gen_random_uuid() not null,
    add column slettet_dato date;
