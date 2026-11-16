do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on tilskudd to "datastream";
            alter publication "ds_publication" add table tilskudd;
        end if;
    end
$$;
