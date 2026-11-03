do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on tilskudd_opplaering to "datastream";
            alter publication "ds_publication" add table tilskudd_opplaering;

            grant select on tilskudd_vedtak to "datastream";
            alter publication "ds_publication" add table tilskudd_vedtak;

            grant select on tilskudd_behandling to "datastream";
            alter publication "ds_publication" add table tilskudd_behandling;

            grant select on bruker_utbetaling to "datastream";
            alter publication "ds_publication" add table bruker_utbetaling;
        end if;
    end
$$;
