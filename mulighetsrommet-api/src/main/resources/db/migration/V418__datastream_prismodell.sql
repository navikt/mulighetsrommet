do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on prismodell to "datastream";
            alter publication "ds_publication" add table prismodell;
        end if;
    end
$$;

do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on avtale_prismodell to "datastream";
            alter publication "ds_publication" add table avtale_prismodell;
        end if;
    end
$$;
