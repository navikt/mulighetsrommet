create type utbetaling_enkeltvedtak_status as enum (
    'OPPRETTET',
    'SENDT',
    'UTBETALT');

create table utbetaling_enkeltvedtak
(
    id                  uuid primary key,
    vedtak_id           uuid                                  not null,
    lopenummer          int                                   not null,
    status              utbetaling_enkeltvedtak_status        not null,
    tiltakstype_id      uuid                                  not null,
    tilskudd_id         uuid                                  not null,
    belop               int                                   not null,
    fakturaStatus       text,
    fakturaFeilmelding  text,
    totrinnskontroll_id uuid                                  not null,
    created_at          timestamptz default current_timestamp not null,
    updated_at          timestamptz default current_timestamp not null,
    foreign key (tiltakstype_id) references tiltakstype (id),
    foreign key (tilskudd_id) references tilskudd_opplaering (id),
    foreign key (totrinnskontroll_id) references totrinnskontroll (id)
);

create or replace function set_utbetaling_enkeltvedtak_lopenummer()
    returns trigger as
$$
begin
    new.lopenummer := coalesce(
            (select max(lopenummer) + 1
             from utbetaling_enkeltvedtak
             where vedtak_id = new.vedtak_id),
            1
                      );
    return new;
end;
$$ language plpgsql;

create trigger set_lopenummer
    before insert
    on utbetaling_enkeltvedtak
    for each row
execute function set_utbetaling_enkeltvedtak_lopenummer();

create trigger set_timestamp
    before update
    on utbetaling_enkeltvedtak
    for each row
execute procedure trigger_set_timestamp();
