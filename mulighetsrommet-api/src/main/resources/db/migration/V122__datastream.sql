DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 from pg_replication_slots where slot_name = 'ds_replication') AND
           EXISTS (SELECT 1 from pg_roles where rolname = 'datastream')
        THEN
            SELECT PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');
        END IF;
    END
$$;
