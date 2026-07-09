ALTER TYPE tilgjengelighetsstatus RENAME VALUE 'Ledig' TO 'LEDIG';
ALTER TYPE tilgjengelighetsstatus RENAME VALUE 'Stengt' TO 'STENGT';
ALTER TYPE tilgjengelighetsstatus RENAME VALUE 'Venteliste' TO 'VENTELISTE';

create or replace function update_tilgjengelighet()
    returns trigger as
$$
begin
    new.tilgjengelighet :=
            case
                when new.tilgjengelighet = 'STENGT' then 'STENGT'
                when new.avslutningsstatus != 'IKKE_AVSLUTTET' then 'STENGT'
                when new.antall_plasser is null then 'LEDIG'
                when new.tilgjengelighet = 'STENGT' then 'STENGT'
                when new.avslutningsstatus != 'IKKE_AVSLUTTET' then 'STENGT'
                when new.antall_plasser is null then 'LEDIG'
                when (select count(*) as antall_deltakere
                      from deltaker
                      where tiltaksgjennomforing_id = new.id
                        and status = 'DELTAR') < new.antall_plasser then 'LEDIG'
                else 'VENTELISTE'
                end::tilgjengelighetsstatus;
    return new;
end
$$ language plpgsql;


create or replace function update_tilgjengelighet_after_deltakelse()
    returns trigger as
$$
begin
    update tiltaksgjennomforing t
    set tilgjengelighet =
            case
                when t.tilgjengelighet = 'STENGT' then 'STENGT'
                when t.avslutningsstatus != 'IKKE_AVSLUTTET' then 'STENGT'
                when t.antall_plasser is null then 'LEDIG'
                when (select count(*) as antall_deltakere
                      from deltaker
                      where tiltaksgjennomforing_id = new.tiltaksgjennomforing_id
                        and status = 'DELTAR') < t.antall_plasser then 'LEDIG'
                else 'VENTELISTE'
                end::tilgjengelighetsstatus
    where t.id = new.tiltaksgjennomforing_id;
    return new;
end
$$ language plpgsql;
