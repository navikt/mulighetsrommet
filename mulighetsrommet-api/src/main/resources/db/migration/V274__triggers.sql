drop trigger if exists set_timestamp on tiltakstype;
create trigger set_timestamp
    before update
    on tiltakstype
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on avtale;
create trigger set_timestamp
    before update
    on avtale
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on veileder_joyride;
create trigger set_timestamp
    before update
    on veileder_joyride
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on deltaker_registrering_innholdselement;
create trigger set_timestamp
    before update
    on deltaker_registrering_innholdselement
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on tiltakstype_deltaker_registrering_innholdselement;
create trigger set_timestamp
    before update
    on tiltakstype_deltaker_registrering_innholdselement
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on arrangor;
create trigger set_timestamp
    before update
    on arrangor
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on avtale_opsjon_logg;
create trigger set_timestamp
    before update
    on avtale_opsjon_logg
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on lagret_filter;
create trigger set_timestamp
    before update
    on lagret_filter
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on avtale_administrator;
create trigger set_timestamp
    before update
    on avtale_administrator
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on avtale_arrangor_underenhet;
create trigger set_timestamp
    before update
    on avtale_arrangor_underenhet
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on avtale_nav_enhet;
create trigger set_timestamp
    before update
    on avtale_nav_enhet
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on del_med_bruker;
create trigger set_timestamp
    before update
    on del_med_bruker
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on deltaker;
create trigger set_timestamp
    before update
    on deltaker
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on gjennomforing;
create trigger set_timestamp
    before update
    on gjennomforing
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on gjennomforing_administrator;
create trigger set_timestamp
    before update
    on gjennomforing_administrator
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on gjennomforing_nav_enhet;
create trigger set_timestamp
    before update
    on gjennomforing_nav_enhet
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on utdanningsprogram;
create trigger set_timestamp
    before update
    on utdanningsprogram
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on utdanning;
create trigger set_timestamp
    before update
    on utdanning
    for each row
execute procedure trigger_set_timestamp();
