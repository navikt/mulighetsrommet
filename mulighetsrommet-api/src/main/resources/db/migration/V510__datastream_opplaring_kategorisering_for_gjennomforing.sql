do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on opplaring_kategorisering to "datastream";
            alter publication "ds_publication" add table opplaring_kategorisering;

            grant select on opplaring_kategorisering_utdanning to "datastream";
            alter publication "ds_publication" add table opplaring_kategorisering_utdanning;
        end if;
    end
$$;
