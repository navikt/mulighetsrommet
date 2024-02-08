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

DROP TRIGGER IF EXISTS set_timestamp ON tiltakshistorikk;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON tiltakshistorikk
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

DROP TRIGGER IF EXISTS set_timestamp ON avtale;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON avtale
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

DROP TRIGGER IF EXISTS set_timestamp ON utkast;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON utkast
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

DROP TRIGGER IF EXISTS set_timestamp ON avtale_notat;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON avtale_notat
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

DROP TRIGGER IF EXISTS set_timestamp ON veileder_joyride;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON veileder_joyride
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON deltaker_registrering_innholdselement
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON tiltakstype_deltaker_registrering_innholdselement
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();
