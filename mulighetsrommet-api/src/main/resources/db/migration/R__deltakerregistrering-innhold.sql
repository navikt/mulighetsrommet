/**
 * Script for å populere tabeller med strukturert
 *  informasjon om hvilke type innhold en gitt tiltakstype kan forvente å innhold.
 *  Bruker blant annet av Komet sin påmeldingsløsning
 */

update tiltakstype
set deltaker_registrering_ledetekst = 'Arbeidsforberedende trening er et tilbud for deg som først ønsker å jobbe i et tilrettelagt arbeidsmiljø. Du får veiledning og støtte av en veileder. Sammen kartlegger dere hvordan din kompetanse, interesser og ferdigheter påvirker muligheten din til å jobbe.'
where tiltakskode = 'ARBFORB';

update tiltakstype
set deltaker_registrering_ledetekst = 'Arbeidsrettet rehabilitering fokuserer på din helse og muligheter i arbeidslivet. Du får veiledning og støtte av en veileder. Sammen kartlegger dere hvordan din helse, kompetanse, interesser og ferdigheter påvirker muligheten din til å jobbe.'
where tiltakskode = 'ARBRRHDAG';

update tiltakstype
set deltaker_registrering_ledetekst = 'Avklaring skal hjelpe deg med å se hva du kan jobbe med. Du har samtaler med en veileder. Sammen kartlegger dere hvordan kompetanse, opplevelser fra tidligere arbeidsplass, interesser og ferdigheter påvirker muligheten din til å jobbe.'
where tiltakskode = 'AVKLARAG';

update tiltakstype
set deltaker_registrering_ledetekst = 'Du får tett oppfølging og støtte av en veileder. Sammen kartlegger dere hvordan din kompetanse, interesser og ferdigheter påvirker muligheten din til å jobbe.'
where tiltakskode = 'INDOPPFAG';

update tiltakstype
set deltaker_registrering_ledetekst = 'Varig tilrettelagt arbeid er et tilbud for deg som får uføretrygd. Du jobber i en skjermet bedrift med arbeidsoppgaver som er tilpasset deg.'
where tiltakskode = 'VASV';

--- AFT = ARBFORB
--- ARR = ARBRRHDAG
--- Avklaring = AVKLARAG
--- Oppfølging = INDOPPFAG
--- VTA = VASV

insert into deltaker_registrering_innholdselement(innholdskode, tekst)
values ('jobbsoking', 'Støtte til jobbsøking'),
       ('arbeidspraksis', 'Arbeidspraksis'),
       ('karriereveiledning', 'Karriereveiledning'),
       ('kartlegge-helse', 'Kartlegge hvordan helsen din påvirker muligheten din til å jobbe'),
       ('kartlegge-forventninger', 'Kartlegge dine forventninger til å jobbe'),
       ('kartlegge-arbeidsplassen', 'Kartlegge hvilken støtte og tilpasning du trenger på arbeidsplassen'),
       ('kartlegge-delta-tiltak', 'Kartlegge hvilken støtte du trenger for å delta på et arbeidsmarkedstiltak'),
       ('kartlegge-grunnleggende-ferdigheter',
        'Kartlegging av grunnleggende ferdigheter: lesing, skriving, regning, muntlige ferdigheter og digitale ferdigheter'),
       ('veiledning-livsstil', 'Veiledning om livsstil og kosthold'),
       ('motivasjon', 'Motivasjons- og mestringsaktiviteter'),
       ('veiledning-sosialt', 'Veiledning i sosial mestring'),
       ('veiledning-trening', 'Individuelt treningsopplegg med veiledning'),
       ('oppfolging-arbeidsplassen', 'Oppfølging på arbeidsplassen'),
       ('veiledning-arbeidsgiver', 'Veiledning til arbeidsgiver'),
       ('tilpasse-arbeidsoppgaver', 'Hjelp til å tilpasse arbeidsoppgaver og arbeidsplassen')
on conflict(innholdskode) do update
    set tekst = excluded.tekst;

--- Sett innhold for AFT (ARBFORB)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('arbeidspraksis', 'ARBFORB'),
             ('karriereveiledning', 'ARBFORB'),
             ('kartlegge-helse', 'ARBFORB'),
             ('kartlegge-grunnleggende-ferdigheter', 'ARBFORB'),
             ('motivasjon', 'ARBFORB'),
             ('veiledning-sosialt', 'ARBFORB'),
             ('oppfolging-arbeidsplassen', 'ARBFORB'),
             ('tilpasse-arbeidsoppgaver', 'ARBFORB')) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'ARBFORB')
on conflict (innholdskode, tiltakskode) do nothing;

--- Sett innhold for ARR (ARBRRHDAG)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('arbeidspraksis', 'ARBRRHDAG'),
             ('kartlegge-helse', 'ARBRRHDAG'),
             ('kartlegge-forventninger', 'ARBRRHDAG'),
             ('kartlegge-arbeidsplassen', 'ARBRRHDAG'),
             ('veiledning-livsstil', 'ARBRRHDAG'),
             ('motivasjon', 'ARBRRHDAG'),
             ('veiledning-sosialt', 'ARBRRHDAG'),
             ('veiledning-trening', 'ARBRRHDAG'),
             ('oppfolging-arbeidsplassen', 'ARBRRHDAG'),
             ('veiledning-arbeidsgiver', 'ARBRRHDAG'),
             ('tilpasse-arbeidsoppgaver', 'ARBRRHDAG')) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'ARBRRHDAG')
on conflict (innholdskode, tiltakskode) do nothing;

--- Sett innhold for Avklaring (AVKLARAG)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('arbeidspraksis', 'AVKLARAG'),
             ('karriereveiledning', 'AVKLARAG'),
             ('kartlegge-helse', 'AVKLARAG'),
             ('kartlegge-forventninger', 'AVKLARAG'),
             ('kartlegge-arbeidsplassen', 'AVKLARAG'),
             ('kartlegge-delta-tiltak', 'AVKLARAG'),
             ('kartlegge-grunnleggende-ferdigheter', 'AVKLARAG'),
             ('oppfolging-arbeidsplassen', 'AVKLARAG'),
             ('veiledning-arbeidsgiver', 'AVKLARAG')) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'AVKLARAG')
on conflict (innholdskode, tiltakskode) do nothing;

--- Sett innhold for Oppfølging (INDOPPFAG)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('jobbsoking', 'INDOPPFAG'),
             ('arbeidspraksis', 'INDOPPFAG'),
             ('karriereveiledning', 'INDOPPFAG'),
             ('kartlegge-helse', 'INDOPPFAG'),
             ('kartlegge-forventninger', 'INDOPPFAG'),
             ('kartlegge-arbeidsplassen', 'INDOPPFAG'),
             ('veiledning-sosialt', 'INDOPPFAG'),
             ('oppfolging-arbeidsplassen', 'INDOPPFAG'),
             ('veiledning-arbeidsgiver', 'INDOPPFAG'),
             ('tilpasse-arbeidsoppgaver', 'INDOPPFAG')) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'INDOPPFAG')
on conflict (innholdskode, tiltakskode) do nothing;



