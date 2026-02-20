-- For hver gjennomføring med type 'ENKELTPLASS', opprett en prismodell og koble til gjennomføringen
do $$
declare
    gj record;
    new_prismodell_id uuid;
begin
    for gj in select id from gjennomforing where gjennomforing_type = 'ENKELTPLASS' and prismodell_id is null
    loop
        insert into prismodell (id, prismodell_type, prisbetingelser, satser, system_id, valuta)
        values (gen_random_uuid(), 'ANNEN_AVTALT_PRIS', null, null, null, 'NOK')
        returning id into new_prismodell_id;

        update gjennomforing set prismodell_id = new_prismodell_id where id = gj.id;
    end loop;
end $$;
