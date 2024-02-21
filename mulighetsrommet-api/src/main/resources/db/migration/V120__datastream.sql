DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'mulighetsrommet-api')
        THEN
            ALTER USER "mulighetsrommet-api" WITH REPLICATION;
        END IF;
    END
$$;

DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'datastream')
        THEN
            ALTER DEFAULT PRIVILEGES IN SCHEMA PUBLIC GRANT SELECT ON TABLES TO "datastream";
            GRANT SELECT ON tiltakstype TO "datastream";

            ALTER USER "datastream" WITH REPLICATION;
        END IF;
    END
$$;
