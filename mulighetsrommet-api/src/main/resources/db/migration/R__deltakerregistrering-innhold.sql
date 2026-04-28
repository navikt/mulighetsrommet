/**
 * Script for å populere tabeller med strukturert
 *  informasjon om hvilke type innhold en gitt tiltakstype kan forvente å innhold.
 *  Bruker blant annet av Komet sin påmeldingsløsning
 */

update tiltakstype
set deltaker_registrering_ledetekst = 'Arbeidsforberedende trening er et tilbud for deg som først ønsker å jobbe i et tilrettelagt arbeidsmiljø. Du får veiledning og støtte av en veileder. Sammen kartlegger dere hvordan din kompetanse, interesser og ferdigheter påvirker muligheten din til å jobbe.'
where tiltakskode = 'ARBEIDSFORBEREDENDE_TRENING';

update tiltakstype
set deltaker_registrering_ledetekst = 'Arbeidsrettet rehabilitering fokuserer på din helse og muligheter i arbeidslivet. Du får veiledning og støtte av en veileder. Sammen kartlegger dere hvordan din helse, kompetanse, interesser og ferdigheter påvirker muligheten din til å jobbe.'
where tiltakskode = 'ARBEIDSRETTET_REHABILITERING';

update tiltakstype
set deltaker_registrering_ledetekst = 'Avklaring skal hjelpe deg med å se hva du kan jobbe med. Du har samtaler med en veileder. Sammen kartlegger dere hvordan kompetanse, opplevelser fra tidligere arbeidsplass, interesser og ferdigheter påvirker muligheten din til å jobbe.'
where tiltakskode = 'AVKLARING';

update tiltakstype
set deltaker_registrering_ledetekst = 'Du får tett oppfølging og støtte av en veileder for å skaffe eller beholde jobb. Sammen ser dere på hvordan din kompetanse, interesser og ferdigheter kan gi deg jobbmuligheter.'
where tiltakskode = 'OPPFOLGING';

update tiltakstype
set deltaker_registrering_ledetekst = 'Varig tilrettelagt arbeid er et tilbud for deg som får uføretrygd. Du jobber i en skjermet bedrift med arbeidsoppgaver som er tilpasset deg.'
where tiltakskode = 'VARIG_TILRETTELAGT_ARBEID_SKJERMET';

insert into deltaker_registrering_innholdselement(innholdskode, tekst)
values ('jobbsoking', 'Støtte til å søke jobber'),
       ('arbeidspraksis', 'Arbeidspraksis'),
       ('karriereveiledning', 'Karriereveiledning'),
       ('kartlegge-helse', 'Kartlegge hvordan helsen din påvirker muligheten din til å jobbe'),
       ('kartlegge-forventninger', 'Kartlegge dine forventninger til å jobbe'),
       ('kartlegge-arbeidsplassen', 'Kartlegge hvilken støtte og tilpasning du trenger på arbeidsplassen'),
       ('kartlegge-delta-tiltak', 'Kartlegge hvilken støtte du trenger for å delta på et arbeidsmarkedstiltak'),
       ('kartlegge-grunnleggende-ferdigheter',
        'Kartlegge grunnleggende ferdigheter som språk og hvordan du leser, skriver, regner og bruker datamaskin'),
       ('veiledning-livsstil', 'Veiledning om livsstil og kosthold'),
       ('motivasjon', 'Motivasjons- og mestringsaktiviteter'),
       ('veiledning-sosialt', 'Veiledning i sosial mestring'),
       ('veiledning-trening', 'Individuelt treningsopplegg med veiledning'),
       ('oppfolging-arbeidsplassen', 'Oppfølging på arbeidsplassen'),
       ('veiledning-arbeidsgiver', 'Veiledning til arbeidsgiver'),
       ('tilpasse-arbeidsoppgaver', 'Hjelp til å tilpasse arbeidsoppgaver og arbeidsplassen')
on conflict(innholdskode) do update
    set tekst = excluded.tekst;

--- Sett innhold for AFT (ARBEIDSFORBEREDENDE_TRENING)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('arbeidspraksis', 'ARBEIDSFORBEREDENDE_TRENING'),
             ('karriereveiledning', 'ARBEIDSFORBEREDENDE_TRENING'),
             ('kartlegge-helse', 'ARBEIDSFORBEREDENDE_TRENING'),
             ('kartlegge-grunnleggende-ferdigheter', 'ARBEIDSFORBEREDENDE_TRENING'),
             ('veiledning-sosialt', 'ARBEIDSFORBEREDENDE_TRENING'),
             ('oppfolging-arbeidsplassen', 'ARBEIDSFORBEREDENDE_TRENING'),
             ('tilpasse-arbeidsoppgaver', 'ARBEIDSFORBEREDENDE_TRENING')) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'ARBEIDSFORBEREDENDE_TRENING')
on conflict (innholdskode, tiltakskode) do nothing;

--- Sett innhold for ARR (ARBEIDSRETTET_REHABILITERING)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('arbeidspraksis', 'ARBEIDSRETTET_REHABILITERING'),
             ('kartlegge-helse', 'ARBEIDSRETTET_REHABILITERING'),
             ('kartlegge-forventninger', 'ARBEIDSRETTET_REHABILITERING'),
             ('kartlegge-arbeidsplassen', 'ARBEIDSRETTET_REHABILITERING'),
             ('veiledning-livsstil', 'ARBEIDSRETTET_REHABILITERING'),
             ('motivasjon', 'ARBEIDSRETTET_REHABILITERING'),
             ('veiledning-sosialt', 'ARBEIDSRETTET_REHABILITERING'),
             ('veiledning-trening', 'ARBEIDSRETTET_REHABILITERING'),
             ('oppfolging-arbeidsplassen', 'ARBEIDSRETTET_REHABILITERING'),
             ('veiledning-arbeidsgiver', 'ARBEIDSRETTET_REHABILITERING'),
             ('tilpasse-arbeidsoppgaver', 'ARBEIDSRETTET_REHABILITERING')) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'ARBEIDSRETTET_REHABILITERING')
on conflict (innholdskode, tiltakskode) do nothing;

--- Sett innhold for Avklaring (AVKLARING)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('karriereveiledning', 'AVKLARING'),
             ('kartlegge-helse', 'AVKLARING'),
             ('kartlegge-forventninger', 'AVKLARING'),
             ('kartlegge-arbeidsplassen', 'AVKLARING'),
             ('kartlegge-delta-tiltak', 'AVKLARING'),
             ('kartlegge-grunnleggende-ferdigheter', 'AVKLARING'),
             ('oppfolging-arbeidsplassen', 'AVKLARING'),
             ('veiledning-arbeidsgiver', 'AVKLARING')) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'AVKLARING')
on conflict (innholdskode, tiltakskode) do nothing;

--- Sett innhold for Oppfølging (OPPFOLGING)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('jobbsoking', 'OPPFOLGING'),
             ('arbeidspraksis', 'OPPFOLGING'),
             ('karriereveiledning', 'OPPFOLGING'),
             ('kartlegge-helse', 'OPPFOLGING'),
             ('kartlegge-forventninger', 'OPPFOLGING'),
             ('kartlegge-arbeidsplassen', 'OPPFOLGING'),
             ('veiledning-sosialt', 'OPPFOLGING'),
             ('oppfolging-arbeidsplassen', 'OPPFOLGING'),
             ('veiledning-arbeidsgiver', 'OPPFOLGING'),
             ('tilpasse-arbeidsoppgaver', 'OPPFOLGING')) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'OPPFOLGING')
on conflict (innholdskode, tiltakskode) do nothing;



