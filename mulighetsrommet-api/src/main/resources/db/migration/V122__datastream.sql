DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 from pg_roles where rolname = 'datastream')
        THEN
            SELECT PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');
        END IF;
    END
$$;
