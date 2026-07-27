drop view if exists view_avtale;
drop view if exists view_opplaring_kategorisering;
drop view if exists view_gjennomforing_avtale_detaljer;

alter table utdanning
    alter column navn set data type text collate "nb-NO-x-icu";

alter table utdanningsprogram
    alter column navn set data type text collate "nb-NO-x-icu";

alter table opplaring_kategorisering_kurstype
    alter column navn set data type text collate "nb-NO-x-icu";

alter table opplaring_kategorisering_bransje
    alter column navn set data type text collate "nb-NO-x-icu";

alter table opplaring_forerkort
    alter column navn set data type text collate "nb-NO-x-icu";

alter table opplaring_innhold_element
    alter column navn set data type text collate "nb-NO-x-icu";
