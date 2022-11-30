alter table tiltaksgjennomforing
    add tilgjengelighet tilgjengelighetsstatus not null default 'Ledig',
    add antall_plasser  int;

create or replace function update_tilgjengelighet()
    returns trigger as
$$
begin
    new.tilgjengelighet :=
            case
                when new.tilgjengelighet = 'Stengt' then 'Stengt'
                when new.avslutningsstatus != 'IKKE_AVSLUTTET' then 'Stengt'
                when new.antall_plasser is null then 'Ledig'
                when (select count(*) as antall_deltakere
                      from deltaker
                      where tiltaksgjennomforing_id = new.id
                        and status = 'DELTAR') < new.antall_plasser then 'Ledig'
                else 'Venteliste'
                end::tilgjengelighetsstatus;
    return new;
end
$$ language plpgsql;

create trigger update_tilgjengelighet
    before insert or update of tilgjengelighet, antall_plasser
    on tiltaksgjennomforing
    for each row
execute procedure update_tilgjengelighet();

create or replace function update_tilgjengelighet_after_deltakelse()
    returns trigger as
$$
begin
    update tiltaksgjennomforing t
    set tilgjengelighet =
            case
                when t.tilgjengelighet = 'Stengt' then 'Stengt'
                when t.avslutningsstatus != 'IKKE_AVSLUTTET' then 'Stengt'
                when t.antall_plasser is null then 'Ledig'
                when (select count(*) as antall_deltakere
                      from deltaker
                      where tiltaksgjennomforing_id = new.tiltaksgjennomforing_id
                        and status = 'DELTAR') < t.antall_plasser then 'Ledig'
                else 'Venteliste'
                end::tilgjengelighetsstatus
    where t.id = new.tiltaksgjennomforing_id;

    return new;
end
$$ language plpgsql;

create trigger update_tilgjengelighet_after_deltakelse
    after insert or update
    on deltaker
    for each row
execute procedure update_tilgjengelighet_after_deltakelse();
