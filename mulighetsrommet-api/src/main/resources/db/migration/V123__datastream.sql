DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'datastream')
        THEN
            GRANT SELECT ON tiltaksgjennomforing TO "datastream";
            CREATE PUBLICATION "ds_publication" FOR TABLE tiltaksgjennomforing;
        END IF;
    END
$$;
