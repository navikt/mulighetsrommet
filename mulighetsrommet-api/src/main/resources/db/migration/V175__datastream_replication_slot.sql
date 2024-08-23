do
$$
    begin
        if not exists (select 1 from pg_replication_slots where slot_name = 'ds_replication') and
           exists (select 1 from pg_roles where rolname = 'datastream')
        then
            select pg_create_logical_replication_slot('ds_replication', 'pgoutput');
        end if;
    end
$$;
