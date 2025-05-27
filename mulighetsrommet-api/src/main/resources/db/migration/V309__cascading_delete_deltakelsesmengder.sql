alter table deltaker_deltakelsesmengde
    drop constraint if exists deltaker_deltakelsesmengde_deltaker_id_fkey;

alter table deltaker_deltakelsesmengde
    add constraint deltaker_deltakelsesmengde_deltaker_id_fkey
        foreign key (deltaker_id) references deltaker (id) on delete cascade;
