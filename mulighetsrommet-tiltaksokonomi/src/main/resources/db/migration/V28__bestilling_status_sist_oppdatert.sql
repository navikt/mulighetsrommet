alter table bestilling
    add column status_sist_oppdatert timestamptz;

update bestilling
set status_sist_oppdatert = updated_at
where status_sist_oppdatert is null;

alter table bestilling
    alter column status_sist_oppdatert set not null;
