/**
 * Script for å populere tabeller med strukturert informasjon om
 * hvilke personopplysninger avtaler på en gitt tiltakstype kan velge mellom.
 */

insert into tiltakstype_personopplysning(tiltakskode, personopplysning, frekvens)
select * from (values
    ('AVKLARING'::tiltakskode, 'NAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'KJONN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'ADRESSE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'TELEFONNUMMER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'FOLKEREGISTER_IDENTIFIKATOR'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'FODSELSDATO'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'BEHOV_FOR_BISTAND_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'YTELSER_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'BILDE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'EPOST'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'BRUKERNAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'ARBEIDSERFARING_OG_VERV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'SERTIFIKATER_OG_KURS'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'IP_ADRESSE'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'UTDANNING_OG_FAGBREV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'PERSONLIGE_EGENSKAPER_OG_INTERESSER', 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'SPRAKKUNNSKAP', 'ALLTID'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'ADFERD'::personopplysning, 'SJELDEN'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'SOSIALE_FORHOLD'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'HELSEOPPLYSNINGER'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('AVKLARING'::tiltakskode, 'RELIGION'::personopplysning, 'SJELDEN'::personopplysning_frekvens)
) as t(tiltakskode, personvernopplysning, frekvens)
where exists (select 1 from tiltakstype where tiltakskode = 'AVKLARING'::tiltakskode)
on conflict(tiltakskode, personopplysning) do update
    set frekvens = excluded.frekvens;

insert into tiltakstype_personopplysning(tiltakskode, personopplysning, frekvens)
select * from (values
    ('OPPFOLGING'::tiltakskode, 'NAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'KJONN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'ADRESSE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'TELEFONNUMMER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'FOLKEREGISTER_IDENTIFIKATOR'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'FODSELSDATO'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'BEHOV_FOR_BISTAND_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'YTELSER_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'BILDE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'EPOST'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'BRUKERNAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'ARBEIDSERFARING_OG_VERV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'SERTIFIKATER_OG_KURS'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'IP_ADRESSE'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'UTDANNING_OG_FAGBREV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'PERSONLIGE_EGENSKAPER_OG_INTERESSER', 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'SPRAKKUNNSKAP', 'ALLTID'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'ADFERD'::personopplysning, 'SJELDEN'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'SOSIALE_FORHOLD'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'HELSEOPPLYSNINGER'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('OPPFOLGING'::tiltakskode, 'RELIGION'::personopplysning, 'SJELDEN'::personopplysning_frekvens)
) as t(tiltakskode, personvernopplysning, frekvens)
where exists (select 1 from tiltakstype where tiltakskode = 'OPPFOLGING'::tiltakskode)
on conflict(tiltakskode, personopplysning) do update
    set frekvens = excluded.frekvens;

insert into tiltakstype_personopplysning(tiltakskode, personopplysning, frekvens)
select * from (values
    ('JOBBKLUBB'::tiltakskode, 'NAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'KJONN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'ADRESSE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'TELEFONNUMMER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'FOLKEREGISTER_IDENTIFIKATOR'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'FODSELSDATO'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'BEHOV_FOR_BISTAND_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'YTELSER_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'BILDE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'EPOST'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'BRUKERNAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'ARBEIDSERFARING_OG_VERV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'SERTIFIKATER_OG_KURS'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'IP_ADRESSE'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'UTDANNING_OG_FAGBREV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'PERSONLIGE_EGENSKAPER_OG_INTERESSER', 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'SPRAKKUNNSKAP', 'ALLTID'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'ADFERD'::personopplysning, 'SJELDEN'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'SOSIALE_FORHOLD'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'HELSEOPPLYSNINGER'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('JOBBKLUBB'::tiltakskode, 'RELIGION'::personopplysning, 'SJELDEN'::personopplysning_frekvens)
) as t(tiltakskode, personvernopplysning, frekvens)
where exists (select 1 from tiltakstype where tiltakskode = 'JOBBKLUBB'::tiltakskode)
on conflict(tiltakskode, personopplysning) do update
    set frekvens = excluded.frekvens;

insert into tiltakstype_personopplysning(tiltakskode, personopplysning, frekvens)
select * from (values
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'NAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'KJONN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'ADRESSE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'TELEFONNUMMER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'FOLKEREGISTER_IDENTIFIKATOR'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'FODSELSDATO'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'BEHOV_FOR_BISTAND_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'YTELSER_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'BILDE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'EPOST'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'BRUKERNAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'ARBEIDSERFARING_OG_VERV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'SERTIFIKATER_OG_KURS'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'IP_ADRESSE'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'UTDANNING_OG_FAGBREV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'PERSONLIGE_EGENSKAPER_OG_INTERESSER', 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'SPRAKKUNNSKAP', 'ALLTID'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'ADFERD'::personopplysning, 'SJELDEN'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'SOSIALE_FORHOLD'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'HELSEOPPLYSNINGER'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode, 'RELIGION'::personopplysning, 'SJELDEN'::personopplysning_frekvens)
) as t(tiltakskode, personvernopplysning, frekvens)
where exists (select 1 from tiltakstype where tiltakskode = 'DIGITALT_OPPFOLGINGSTILTAK'::tiltakskode)
on conflict(tiltakskode, personopplysning) do update
    set frekvens = excluded.frekvens;

insert into tiltakstype_personopplysning(tiltakskode, personopplysning, frekvens)
select * from (values
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'NAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'KJONN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'ADRESSE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'TELEFONNUMMER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'FOLKEREGISTER_IDENTIFIKATOR'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'FODSELSDATO'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'BEHOV_FOR_BISTAND_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'YTELSER_FRA_NAV'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'BILDE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'EPOST'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'BRUKERNAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'ARBEIDSERFARING_OG_VERV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'SERTIFIKATER_OG_KURS'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'IP_ADRESSE'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'UTDANNING_OG_FAGBREV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'PERSONLIGE_EGENSKAPER_OG_INTERESSER', 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'SPRAKKUNNSKAP', 'OFTE'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'ADFERD'::personopplysning, 'SJELDEN'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'SOSIALE_FORHOLD'::personopplysning, 'SJELDEN'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'HELSEOPPLYSNINGER'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode, 'RELIGION'::personopplysning, 'SJELDEN'::personopplysning_frekvens)
) as t(tiltakskode, personvernopplysning, frekvens)
where exists (select 1 from tiltakstype where tiltakskode = 'GRUPPE_ARBEIDSMARKEDSOPPLAERING'::tiltakskode)
on conflict(tiltakskode, personopplysning) do update
    set frekvens = excluded.frekvens;

insert into tiltakstype_personopplysning(tiltakskode, personopplysning, frekvens)
select * from (values
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'NAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'KJONN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'ADRESSE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'TELEFONNUMMER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'FOLKEREGISTER_IDENTIFIKATOR'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'FODSELSDATO'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'BEHOV_FOR_BISTAND_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'YTELSER_FRA_NAV'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'BILDE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'EPOST'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'BRUKERNAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'ARBEIDSERFARING_OG_VERV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'SERTIFIKATER_OG_KURS'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'IP_ADRESSE'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'UTDANNING_OG_FAGBREV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'PERSONLIGE_EGENSKAPER_OG_INTERESSER', 'ALLTID'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'SPRAKKUNNSKAP', 'OFTE'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'ADFERD'::personopplysning, 'SJELDEN'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'SOSIALE_FORHOLD'::personopplysning, 'SJELDEN'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'HELSEOPPLYSNINGER'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode, 'RELIGION'::personopplysning, 'SJELDEN'::personopplysning_frekvens)
) as t(tiltakskode, personvernopplysning, frekvens)
where exists (select 1 from tiltakstype where tiltakskode = 'GRUPPE_FAG_OG_YRKESOPPLAERING'::tiltakskode)
on conflict(tiltakskode, personopplysning) do update
    set frekvens = excluded.frekvens;

insert into tiltakstype_personopplysning(tiltakskode, personopplysning, frekvens)
select * from (values
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'NAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'KJONN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'ADRESSE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'TELEFONNUMMER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'FOLKEREGISTER_IDENTIFIKATOR'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'FODSELSDATO'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'BEHOV_FOR_BISTAND_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'YTELSER_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'BILDE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'EPOST'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'BRUKERNAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'ARBEIDSERFARING_OG_VERV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'SERTIFIKATER_OG_KURS'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'IP_ADRESSE'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'UTDANNING_OG_FAGBREV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'PERSONLIGE_EGENSKAPER_OG_INTERESSER', 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'SPRAKKUNNSKAP', 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'ADFERD'::personopplysning, 'SJELDEN'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'SOSIALE_FORHOLD'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'HELSEOPPLYSNINGER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSRETTET_REHABILITERING'::tiltakskode, 'RELIGION'::personopplysning, 'SJELDEN'::personopplysning_frekvens)
) as t(tiltakskode, personvernopplysning, frekvens)
where exists (select 1 from tiltakstype where tiltakskode = 'ARBEIDSRETTET_REHABILITERING'::tiltakskode)
on conflict(tiltakskode, personopplysning) do update
    set frekvens = excluded.frekvens;

insert into tiltakstype_personopplysning(tiltakskode, personopplysning, frekvens)
select * from (values
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'NAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'KJONN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'ADRESSE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'TELEFONNUMMER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'FOLKEREGISTER_IDENTIFIKATOR'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'FODSELSDATO'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'BEHOV_FOR_BISTAND_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'YTELSER_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'BILDE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'EPOST'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'BRUKERNAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'ARBEIDSERFARING_OG_VERV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'SERTIFIKATER_OG_KURS'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'IP_ADRESSE'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'UTDANNING_OG_FAGBREV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'PERSONLIGE_EGENSKAPER_OG_INTERESSER', 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'SPRAKKUNNSKAP', 'ALLTID'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'ADFERD'::personopplysning, 'SJELDEN'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'SOSIALE_FORHOLD'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'HELSEOPPLYSNINGER'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('ARBEIDSFORBEREDENDE_TRENING'::tiltakskode, 'RELIGION'::personopplysning, 'SJELDEN'::personopplysning_frekvens)
) as t(tiltakskode, personvernopplysning, frekvens)
where exists (select 1 from tiltakstype where tiltakskode = 'ARBEIDSFORBEREDENDE_TRENING'::tiltakskode)
on conflict(tiltakskode, personopplysning) do update
    set frekvens = excluded.frekvens;

insert into tiltakstype_personopplysning(tiltakskode, personopplysning, frekvens)
select * from (values
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'NAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'KJONN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'ADRESSE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'TELEFONNUMMER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'FOLKEREGISTER_IDENTIFIKATOR'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'FODSELSDATO'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'BEHOV_FOR_BISTAND_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'YTELSER_FRA_NAV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'BILDE'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'EPOST'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'BRUKERNAVN'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'ARBEIDSERFARING_OG_VERV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'SERTIFIKATER_OG_KURS'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'IP_ADRESSE'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'UTDANNING_OG_FAGBREV'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'PERSONLIGE_EGENSKAPER_OG_INTERESSER', 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'SPRAKKUNNSKAP', 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'ADFERD'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'SOSIALE_FORHOLD'::personopplysning, 'OFTE'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'HELSEOPPLYSNINGER'::personopplysning, 'ALLTID'::personopplysning_frekvens),
    ('VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode, 'RELIGION'::personopplysning, 'SJELDEN'::personopplysning_frekvens)
) as t(tiltakskode, personvernopplysning, frekvens)
where exists (select 1 from tiltakstype where tiltakskode = 'VARIG_TILRETTELAGT_ARBEID_SKJERMET'::tiltakskode)
on conflict(tiltakskode, personopplysning) do update
    set frekvens = excluded.frekvens;
