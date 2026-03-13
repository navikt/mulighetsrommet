create table utbetaling_tilskudd
(
    id                  uuid primary key,
    vedtak_id           uuid                                  not null,
    lopenummer          int                                   not null,
    tiltakstype_id      uuid                                  not null,
    tilskudd_id         uuid                                  not null,
    belop               int                                   not null,
    fakturaStatus       text,
    fakturaFeilmelding  text,
    totrinnskontroll_id uuid                                  not null,
    created_at          timestamptz default current_timestamp not null,
    updated_at          timestamptz default current_timestamp not null,
    foreign key (tiltakstype_id) references tiltakstype (id),
    foreign key (tilskudd_id) references tilskudd (id),
    foreign key (totrinnskontroll_id) references totrinnskontroll (id)
);

create or replace function set_utbetaling_tilskudd_lopenummer()
    returns trigger as
$$
begin
    new.lopenummer := coalesce(
            (select max(lopenummer) + 1
             from utbetaling_tilskudd
             where vedtak_id = new.vedtak_id),
            1
                      );
    return new;
end;
$$ language plpgsql;

create trigger set_lopenummer
    before insert
    on utbetaling_tilskudd
    for each row
execute function set_utbetaling_tilskudd_lopenummer();

create trigger set_timestamp
    before update
    on utbetaling_tilskudd
    for each row
execute procedure trigger_set_timestamp();
