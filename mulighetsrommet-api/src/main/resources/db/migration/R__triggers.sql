CREATE OR REPLACE FUNCTION trigger_set_timestamp()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_timestamp ON tiltakstype;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON tiltakstype
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

DROP TRIGGER IF EXISTS set_timestamp ON tiltaksgjennomforing;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON tiltaksgjennomforing
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

DROP TRIGGER IF EXISTS set_timestamp ON avtale;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON avtale
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

DROP TRIGGER IF EXISTS set_timestamp ON veileder_joyride;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON veileder_joyride
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

DROP TRIGGER IF EXISTS set_timestamp ON deltaker_registrering_innholdselement;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON deltaker_registrering_innholdselement
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

DROP TRIGGER IF EXISTS set_timestamp ON tiltakstype_deltaker_registrering_innholdselement;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON tiltakstype_deltaker_registrering_innholdselement
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

drop trigger if exists set_timestamp on arrangor;

create trigger set_timestamp
    before update
    on arrangor
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on utdanning;

create trigger set_timestamp
    before update
    on utdanning
    for each row
execute procedure trigger_set_timestamp();

drop trigger if exists set_timestamp on utdanning_nus_kode_innhold;

create trigger set_timestamp
    before update
    on utdanning_nus_kode_innhold
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

CREATE OR REPLACE FUNCTION next_tilsagn_lopenummer() RETURNS TRIGGER AS $$
DECLARE
    next_lopenummer INT;
BEGIN
    SELECT COALESCE(MAX(lopenummer), 0) + 1 INTO next_lopenummer
    FROM tilsagn
    WHERE tiltaksgjennomforing_id = NEW.tiltaksgjennomforing_id;

    NEW.lopenummer = next_lopenummer;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_tilsagn_lopenummer
BEFORE INSERT ON tilsagn
FOR EACH ROW
EXECUTE FUNCTION next_tilsagn_lopenummer();
