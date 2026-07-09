alter table utkast
    alter column avtale_id drop not null;

alter table utkast
    drop constraint utkast_avtale_id_fkey;
