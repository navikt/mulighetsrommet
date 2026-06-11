alter table virksomhet_kontaktperson
    drop constraint virksomhet_kontaktperson_virksomhet_id_fkey,
    add constraint virksomhet_kontaktperson_virksomhet_id_fkey foreign key (virksomhet_id) references virksomhet (id) on delete cascade;
