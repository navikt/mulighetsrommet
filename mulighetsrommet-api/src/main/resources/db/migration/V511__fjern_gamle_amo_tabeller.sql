drop view if exists view_avtale, view_gjennomforing_avtale_detaljer, view_opplaring_kategorisering;

drop table if exists avtale_amo_kategorisering,
    avtale_amo_kategorisering_forerkort,
    avtale_amo_kategorisering_sertifisering,
    avtale_utdanningsprogram,
    gjennomforing_amo_kategorisering,
    gjennomforing_amo_kategorisering_forerkort,
    gjennomforing_amo_kategorisering_sertifisering,
    gjennomforing_utdanningsprogram;

alter table if exists amo_sertifisering rename to opplaring_sertifisering;
alter table opplaring_sertifisering rename constraint amo_sertifisering_pkey to opplaring_sertifisering_pkey;
