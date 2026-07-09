DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'datastream')
        THEN
            GRANT SELECT ON tiltaksgjennomforing TO "datastream";
            ALTER PUBLICATION "ds_publication" ADD TABLE tiltaksgjennomforing;
        END IF;
    END
$$;
