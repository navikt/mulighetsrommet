alter table oebs_kvittering
    add column created_at timestamptz default current_timestamp not null;
