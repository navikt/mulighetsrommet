do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on del_med_bruker to "datastream";
            alter publication "ds_publication" add table del_med_bruker;
        end if;
    end
$$;
