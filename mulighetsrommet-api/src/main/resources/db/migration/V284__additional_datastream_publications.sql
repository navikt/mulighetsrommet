do
$$
begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on tilsagn to "datastream";
            alter publication "ds_publication" add table tilsagn;

            grant select on delutbetaling to "datastream";
            alter publication "ds_publication" add table delutbetaling;

            grant select on utbetaling to "datastream";
            alter publication "ds_publication" add table utbetaling;
        end if;
end
$$;
