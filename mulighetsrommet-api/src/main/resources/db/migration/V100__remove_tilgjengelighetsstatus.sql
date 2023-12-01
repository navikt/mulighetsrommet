drop trigger update_tilgjengelighet on tiltaksgjennomforing;
drop function update_tilgjengelighet;

drop trigger update_tilgjengelighet_after_deltakelse on deltaker;
drop function update_tilgjengelighet_after_deltakelse;

alter table tiltaksgjennomforing
    drop tilgjengelighet;

drop type tilgjengelighetsstatus;
