-- Gjenoppretter datastream sin logiske replication slot dersom den mangler.
-- Logiske replication slots overlever ikke Cloud SQL restore/failover/gjenoppretting,
-- og datastreamen havner i FAILED_PERMANENTLY dersom sloten forsvinner.
--
-- Idempotent: no-op i miljoer der sloten allerede finnes (f.eks. prod) eller der
-- datastream-oppsettet ikke er tatt i bruk (lokalt/test der 'datastream'-rollen mangler).
--
-- Merk: bruker 'perform' (ikke 'select') fordi 'select' uten 'into' ikke er lovlig i en
-- pl/pgsql-blokk. Dette var en skjult feil i V175, som gjorde at create-grenen der aldri
-- kunne kjore. Appbrukeren har REPLICATION (se V120), saa create er tillatt.
do
$$
    begin
        if not exists (select 1 from pg_replication_slots where slot_name = 'ds_replication')
           and exists (select 1 from pg_roles where rolname = 'datastream')
        then
            perform pg_create_logical_replication_slot('ds_replication', 'pgoutput');
            raise notice 'Opprettet manglende replication slot ds_replication';
        end if;
    end
$$;
