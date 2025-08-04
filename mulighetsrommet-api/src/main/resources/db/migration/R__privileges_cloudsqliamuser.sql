--
-- Dette scriptet kan benyttes til å gi deg midlertidige skriverettigheter (via gruppen cloudsqliamuser) til ønskede tabeller.
--
-- Hver gang scriptet kjøres vil først alle rettigheter fjernes, før de deretter gis på nytt.
-- Det holder derfor å legge til (eller fjerne) grants til ønskede tabeller uten å eksplisitt måtte huske å kjøre `revoke` selv.
--
do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'cloudsqliamuser') then
            -- Fjern alle skriverettigheter
            execute (
                select string_agg('revoke insert, update, delete on table ' || quote_ident(tablename) || ' from cloudsqliamuser;', E'\n')
                from pg_tables
                where schemaname = 'public'
            );

            -- Deretter gir vi skriverettigheter til ønskede tabeller
            grant insert, update, delete on table topics to cloudsqliamuser;
            grant insert, update, delete on table kafka_consumer_record to cloudsqliamuser;
            grant insert, update, delete on table scheduled_tasks to cloudsqliamuser;
        end if;
    end
$$;
