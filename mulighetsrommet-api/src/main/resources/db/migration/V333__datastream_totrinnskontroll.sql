do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on totrinnskontroll to "datastream";
            alter publication "ds_publication" add table totrinnskontroll;
        end if;
    end
$$;
