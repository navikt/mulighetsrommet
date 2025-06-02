do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'cloudsqliamuser') then
            grant insert, update, delete on table topics to cloudsqliamuser;
            grant insert, update, delete on table kafka_consumer_record to cloudsqliamuser;
            grant insert, update, delete on table scheduled_tasks to cloudsqliamuser;
        end if;
    end
$$;
