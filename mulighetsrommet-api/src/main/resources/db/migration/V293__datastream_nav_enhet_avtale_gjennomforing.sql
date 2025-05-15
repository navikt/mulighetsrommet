do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on avtale_nav_enhet to "datastream";
            alter publication "ds_publication" add table avtale_nav_enhet;

            grant select on gjennomforing_nav_enhet to "datastream";
            alter publication "ds_publication" add table gjennomforing_nav_enhet;
        end if;
    end
$$;
