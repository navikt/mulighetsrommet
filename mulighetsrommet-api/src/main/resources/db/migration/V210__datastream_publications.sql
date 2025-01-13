do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on arrangor to "datastream";
            alter publication "ds_publication" add table arrangor;

            grant select on avtale to "datastream";
            alter publication "ds_publication" add table avtale;

            grant select on tiltaksgjennomforing_amo_kategorisering to "datastream";
            alter publication "ds_publication" add table tiltaksgjennomforing_amo_kategorisering;

            grant select on tiltaksgjennomforing_amo_kategorisering_sertifisering to "datastream";
            alter publication "ds_publication" add table tiltaksgjennomforing_amo_kategorisering_sertifisering;

            grant select on tiltaksgjennomforing_utdanningsprogram to "datastream";
            alter publication "ds_publication" add table tiltaksgjennomforing_utdanningsprogram;

            grant select on utdanningsprogram to "datastream";
            alter publication "ds_publication" add table utdanningsprogram;

            grant select on utdanning to "datastream";
            alter publication "ds_publication" add table utdanning;

            grant select on utdanning_nus_kode to "datastream";
            alter publication "ds_publication" add table utdanning_nus_kode;

            grant select on utdanning_nus_kode_innhold to "datastream";
            alter publication "ds_publication" add table utdanning_nus_kode_innhold;
        end if;
    end
$$;
