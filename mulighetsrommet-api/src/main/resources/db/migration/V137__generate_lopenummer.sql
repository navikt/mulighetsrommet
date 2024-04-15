alter table avtale
    add lopenummer text unique;

alter table tiltaksgjennomforing
    add lopenummer text unique;

create or replace function generate_lopenummer() returns trigger as
$$
declare
    year text;
begin
    if new.lopenummer is null then
        year := date_part('year', new.created_at);

        execute ('create sequence if not exists lopenummer_' || year || '_seq as integer minvalue 10000');

        new.lopenummer = year || '/' || nextval('lopenummer_' || year || '_seq');
    end if;
    return new;
end;
$$ language plpgsql;

create or replace trigger avtale_generate_lopenummer
    before insert
    on avtale
    for each row
execute procedure generate_lopenummer();

create or replace trigger tiltaksgjennomforing_generate_lopenummer
    before insert
    on tiltaksgjennomforing
    for each row
execute procedure generate_lopenummer();
