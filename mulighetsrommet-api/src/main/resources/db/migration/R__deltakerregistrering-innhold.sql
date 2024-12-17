/**
 * Script for å populere tabeller med strukturert informasjon om hvilke type innhold en gitt tiltakstype kan forvente å innhold.
 * Brukes blant annet av Komet sin påmeldingsløsning.
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
set deltaker_registrering_ledetekst = 'Du får tett oppfølging og støtte av en veileder. Sammen kartlegger dere hvordan din kompetanse, interesser og ferdigheter påvirker muligheten din til å jobbe.'
where tiltakskode = 'OPPFOLGING';

update tiltakstype
set deltaker_registrering_ledetekst = 'Varig tilrettelagt arbeid er et tilbud for deg som får uføretrygd. Du jobber i en skjermet bedrift med arbeidsoppgaver som er tilpasset deg.'
where tiltakskode = 'VARIG_TILRETTELAGT_ARBEID_SKJERMET';

update tiltakstype
set deltaker_registrering_ledetekst = 'Du får oppfølging og støtte til jobbsøkingen. På kurset får du karriereveiledning, hjelp til å orientere deg på arbeidsmarkedet, skrive CV og jobbsøknad og trene på jobbintervju.'
where tiltakskode = 'DIGITALT_OPPFOLGINGSTILTAK';

insert into deltaker_registrering_innholdselement(innholdskode, tekst)
values ('jobbsoking', 'Støtte til å søke jobber'),
       ('arbeidspraksis', 'Arbeidspraksis'),
       ('arbeidsutproving', 'Arbeidsutprøving'),
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
from (values ('arbeidspraksis', 'ARBEIDSFORBEREDENDE_TRENING'::tiltakskode),
             ('karriereveiledning', 'ARBEIDSFORBEREDENDE_TRENING'::tiltakskode),
             ('kartlegge-helse', 'ARBEIDSFORBEREDENDE_TRENING'::tiltakskode),
             ('kartlegge-grunnleggende-ferdigheter', 'ARBEIDSFORBEREDENDE_TRENING'::tiltakskode),
             ('veiledning-sosialt', 'ARBEIDSFORBEREDENDE_TRENING'::tiltakskode),
             ('oppfolging-arbeidsplassen', 'ARBEIDSFORBEREDENDE_TRENING'::tiltakskode),
             ('tilpasse-arbeidsoppgaver', 'ARBEIDSFORBEREDENDE_TRENING'::tiltakskode)) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'ARBEIDSFORBEREDENDE_TRENING')
on conflict (innholdskode, tiltakskode) do nothing;

--- Sett innhold for ARR (ARBEIDSRETTET_REHABILITERING)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('arbeidsutproving', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode),
             ('kartlegge-helse', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode),
             ('kartlegge-forventninger', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode),
             ('kartlegge-arbeidsplassen', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode),
             ('veiledning-livsstil', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode),
             ('motivasjon', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode),
             ('veiledning-sosialt', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode),
             ('veiledning-trening', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode),
             ('oppfolging-arbeidsplassen', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode),
             ('veiledning-arbeidsgiver', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode),
             ('tilpasse-arbeidsoppgaver', 'ARBEIDSRETTET_REHABILITERING'::tiltakskode)) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'ARBEIDSRETTET_REHABILITERING')
on conflict (innholdskode, tiltakskode) do nothing;

--- Sett innhold for Avklaring (AVKLARING)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('arbeidsutproving', 'AVKLARING'::tiltakskode),
             ('karriereveiledning', 'AVKLARING'::tiltakskode),
             ('kartlegge-helse', 'AVKLARING'::tiltakskode),
             ('kartlegge-forventninger', 'AVKLARING'::tiltakskode),
             ('kartlegge-arbeidsplassen', 'AVKLARING'::tiltakskode),
             ('kartlegge-delta-tiltak', 'AVKLARING'::tiltakskode),
             ('kartlegge-grunnleggende-ferdigheter', 'AVKLARING'::tiltakskode),
             ('oppfolging-arbeidsplassen', 'AVKLARING'::tiltakskode),
             ('veiledning-arbeidsgiver', 'AVKLARING'::tiltakskode)) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'AVKLARING')
on conflict (innholdskode, tiltakskode) do nothing;

--- Sett innhold for Oppfølging (OPPFOLGING)
insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
select *
from (values ('jobbsoking', 'OPPFOLGING'::tiltakskode),
             ('arbeidspraksis', 'OPPFOLGING'::tiltakskode),
             ('karriereveiledning', 'OPPFOLGING'::tiltakskode),
             ('kartlegge-helse', 'OPPFOLGING'::tiltakskode),
             ('kartlegge-forventninger', 'OPPFOLGING'::tiltakskode),
             ('kartlegge-arbeidsplassen', 'OPPFOLGING'::tiltakskode),
             ('veiledning-sosialt', 'OPPFOLGING'::tiltakskode),
             ('oppfolging-arbeidsplassen', 'OPPFOLGING'::tiltakskode),
             ('veiledning-arbeidsgiver', 'OPPFOLGING'::tiltakskode),
             ('tilpasse-arbeidsoppgaver', 'OPPFOLGING'::tiltakskode)) as t(innholdskode, tiltakskode)
where exists (select 1 from tiltakstype where tiltakskode = 'OPPFOLGING')
on conflict (innholdskode, tiltakskode) do nothing;
