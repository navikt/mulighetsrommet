DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 from pg_roles where rolname = 'datastream')
        THEN
            CREATE PUBLICATION "ds_publication" FOR TABLE tiltakstype;
        END IF;
    END
$$;

