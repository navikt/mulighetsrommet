-- 2023-03-23
do
$$
    begin
        if exists
            (select 1 from pg_user where usename = 'sanity-sync-job')
        then
            grant all on all tables in schema public to "sanity-sync-job";
        end if;
    end
$$;
