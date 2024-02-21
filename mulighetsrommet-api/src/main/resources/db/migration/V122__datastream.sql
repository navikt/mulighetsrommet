DO
$$
    BEGIN
        IF NOT EXISTS
                (SELECT 1 from pg_replication_slots where slot_name = 'ds_replication')
        THEN
            SELECT PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');
        END IF;
    END
$$;
