do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on tilsagn_type to "datastream";
            alter publication "ds_publication" add table tilsagn_type;

            grant select on tilsagn_status_type to "datastream";
            alter publication "ds_publication" add table tilsagn_status_type;

            grant select on utbetaling_status_type to "datastream";
            alter publication "ds_publication" add table utbetaling_status_type;

            grant select on utbetaling_linje_status_type to "datastream";
            alter publication "ds_publication" add table utbetaling_linje_status_type;
        end if;
    end
$$;
drop view if exists view_tilsagn;
