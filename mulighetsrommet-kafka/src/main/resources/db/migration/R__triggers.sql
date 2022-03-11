CREATE OR REPLACE FUNCTION trigger_set_timestamp()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_timestamp ON events;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON events
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();
