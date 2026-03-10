-- Fjern kurstype 'STUDIESPESIALISERING, da ny tiltakstype 'Studiespesialisering' dekker samme distinksjon
drop view if exists view_avtale;
drop view if exists view_gjennomforing_avtale_detaljer;

create type amo_kurstype_new as enum ('BRANSJE_OG_YRKESRETTET', 'NORSKOPPLAERING', 'GRUNNLEGGENDE_FERDIGHETER', 'FORBEREDENDE_OPPLAERING_FOR_VOKSNE');

delete from avtale_amo_kategorisering
where kurstype = 'STUDIESPESIALISERING';

alter table avtale_amo_kategorisering
    alter column kurstype type amo_kurstype_new
        using kurstype::text::amo_kurstype_new;

delete from gjennomforing_amo_kategorisering
where kurstype = 'STUDIESPESIALISERING';

alter table gjennomforing_amo_kategorisering
    alter column kurstype type amo_kurstype_new
        using kurstype::text::amo_kurstype_new;


drop type amo_kurstype;

alter type amo_kurstype_new rename to amo_kurstype;

alter type amo_kurstype owner to valp;
