do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            alter publication "ds_publication" drop table delutbetaling;
            revoke select on delutbetaling from "datastream";
        end if;
    end
$$;
