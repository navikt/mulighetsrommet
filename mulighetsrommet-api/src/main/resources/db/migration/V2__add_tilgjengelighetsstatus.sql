create type tilgjengelighetsstatus as enum ('Ledig', 'Venteliste', 'Stengt');

alter table tiltaksgjennomforing
    add apent_for_innsok boolean                not null default true,
    add antall_plasser   int,
    add tilgjengelighet  tilgjengelighetsstatus not null default 'Ledig';

create or replace function update_tilgjengelighet()
    returns trigger as
$$
begin
    new.tilgjengelighet =
            case
                when new.apent_for_innsok = false then 'Stengt'
                when new.antall_plasser is null then 'Ledig'
                when (
                         select count(*) as antall_deltakere
                         from deltaker
                         where tiltaksgjennomforing_id = new.arena_id
                           and status = 'DELTAR'
                     ) < new.antall_plasser then 'Ledig'
                else 'Venteliste'
                end::tilgjengelighetsstatus;
    return new;
end
$$ language plpgsql;

create trigger update_tilgjengelighet
    before insert or update of apent_for_innsok, antall_plasser
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
                when t.apent_for_innsok = false then 'Stengt'
                when t.antall_plasser is null then 'Ledig'
                when (
                         select count(*) as antall_deltakere
                         from deltaker
                         where tiltaksgjennomforing_id = new.tiltaksgjennomforing_id
                           and status = 'DELTAR'
                     ) < t.antall_plasser then 'Ledig'
                else 'Venteliste'
                end::tilgjengelighetsstatus
    where t.arena_id = new.tiltaksgjennomforing_id;

    return new;
end
$$ language plpgsql;

create trigger update_tilgjengelighet_after_deltakelse
    after insert or update
    on deltaker
    for each row
execute procedure update_tilgjengelighet_after_deltakelse();

create or replace view tiltaksgjennomforing_valid(id, navn, tiltaksnummer, tiltakskode, aar, tilgjengelighet) as
select id, navn, tiltaksnummer, tiltakskode, aar, tilgjengelighet
from tiltaksgjennomforing
where tiltaksnummer is not null
  and aar is not null

