INSERT INTO tiltaksvariant
VALUES (1, 'Avklaring',
        '',
        'Avklaring skal kartlegge og vurdere tiltaksdeltakerens arbeidsevne og eventuelle spesifikke behov for bistand for å skaffe seg eller beholde arbeid. Avklaringen skal bidra til at deltaker får økt innsikt i sine muligheter på arbeidsmarkedet og i egne ressurser og ferdigheter i.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (2, 'Arbeidstrening',
        '',
        'Tiltaket skal bidra til å styrke mulighetene for å komme i jobb. Arbeidstrening kan ha flere ulike formål i form av arbeidserfaring for å mestre en bestemt jobb, eller behov for en referanse mens man søker ordinært arbeid.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (3, 'Oppfølging',
        '',
        'Oppfølging skal gi bistand med sikte på at tiltaksdeltakere skaffer seg og/eller beholder, lønnet arbeid. Målet er at tiltaksdeltaker i størst mulig grad skal bli selvforsørget med en varig tilknytning til arbeidslivet.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (4, 'Opplæring', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (5, 'Mentor',
        '',
        'Mentor skal gi nødvendig bistand til å kunne gjennomføre arbeidsmarkedstiltak, eller for å kunne få eller beholde lønnet arbeid i en ordinær bedrift.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (6, 'Funksjonsassistanse i arbeidslivet',
        '',
        'Ordningen skal bidra til at personer med nedsatt fysisk funksjonsevne, blinde eller svaksynte kan skaffe seg eller beholde ordinært arbeid. Ordningen dekker utgifter til nødvendig, praktisk hjelp i arbeidssituasjonen. Blinde og svaksynte kan få funksjonsassistanse til ledsaging.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (7, 'Lønnstilskudd - midlertidig',
        '',
        'Midlertidig lønnstilskudd skal bidra til å øke muligheten for å få jobb for personer som har utfordringer med å komme inn på arbeidsmarkedet, med sikte på et varig arbeidsforhold. Tilskuddet skal bidra til å redusere arbeidsgivers risiko.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (8, 'Lønnstilskudd - varig',
        '',
        'Varig lønnstilskudd skal bidra til å øke mulighetene for å få jobb for personer med varig og vesentlig nedsatt arbeidsevne.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (9, 'Inkluderingstilskudd',
        '',
        'Inkluderingstilskudd skal bidra til å senke terskelen for arbeidsgivere som vil rekruttere eller prøve ut arbeidssøkere, med behov for bistand for å komme i arbeid.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (10, 'Arbeidsrettet rehabilitering (ARR)',
        '',
        'Arbeidsrettet rehabilitering skal styrke den enkeltes arbeidsevne og bidra til mestring av helserelaterte og sosiale problemer som hindrer deltakelse i arbeidslivet. Målet med tiltaket er at deltaker skal komme i eller forbli i arbeid.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (11, 'Arbeidsforberedende trening',
        '',
        'AFT skal bidra til å prøve ut den enkeltes arbeidsevne og til å styrke mulighetene for å få ordinært arbeid.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (12, 'Varig tilrettelagt arbeid i skjermet virksomhet (VTA)',
        '',
        'VTA skal tilby personer arbeid i en skjermet virksomhet, med arbeidsoppgaver tilpasset den enkeltes yteevne.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (13, 'Arbeidsmarkedsopplæring (AMO)',
        '',
        'AMO er kortere kurs med yrkesrettet innhold som er basert på behov i arbeidsmarkedet. Kursene skal bestå av innhold som gir formell kompetanse. Annet innhold som understøtter formålet om kvalifisering av arbeidssøkere, kan inngå som en del av kurset.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (14, 'Arbeids- og utdanningsreiser (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (15, 'Arbeidsrettet veiledningstjeneste (AVT)',
        '',
        'Arbeidsrettet veiledningstjeneste (AVT) ved hjelpemiddelsentralene retter seg mot personer med synsvansker, hørselsvansker og ervervet hjerneskade som har behov for råd og veiledning om hva som skal til for å delta i arbeidslivet.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (16, 'Bedriftsintern opplæring (BIO)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (17, 'Egenetablering (servicerutine AAP)',
        '',
        'Det kan gis arbeidsavklaringspenger under etablering av egen virksomhet når etableringen antas å føre til at brukeren blir selvforsørget, eventuelt i kombinasjon med gradert uføretrygd. Etableringen må gjelde ny virksomhet.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (18, 'Ekspertbistand - tilskudd',
        '',
        'Tilskudd til ekstern ekspertbistand skal støtte opp under arbeidet med å forebygge og redusere sykefravær på den enkelte arbeidsplassen. Hensikten med bistanden er at eksperten skal bidra til å løse problemet som leder til sykefravær, med sikte på å tilbakeføre arbeidstakeren til arbeid.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (19, 'Digitalt oppfølgingstiltak for arbeidsledige (digital jobbklubb)',
        '',
        'Formålet med tiltaket er at permitterte og arbeidsledige raskt skal få bistand til å søke jobb og få karriereveiledning. Tiltaket har lav terskel for innsøking og er teknisk sett en jobbklubb.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (20, 'Fag- og yrkesopplæring på videregående skoles nivå eller høyere yrkesfaglig utdanning',
        '',
        'Formålet med opplæringstiltaket er å bidra til å kvalifisere brukere til arbeid i bransjer med ledige jobber.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (21, 'Friskmelding til arbeidsformidling (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (22, 'Førerhund (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (23, 'Gradert sykmelding (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (24, 'Gravid arbeidstaker (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (25, 'Hjelpemidler og tilrettelegging på arbeidsplassen (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (26, 'Høyere utdanning', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (27, 'IPS Individual Placement and Support (Individuell jobbstøtte)',
        '',
        'Arbeids- og velferdsdirektoratet og Helsedirektoratet har samarbeidet tett om videreutvikling og nasjonal spredning av IPS.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (28, 'Jobbklubb (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (29, 'Jobbklubb: Digitalt oppfølgingstiltak for arbeidsledige (digital jobbklubb)',
        '',
        'Tiltaket er et kortvarig digitalt jobbsøkings- og karriereveiledningskurs som skal gi digital/nettbasert karriereveiledning, individuell oppfølging og annen jobbsøkingsbistand til arbeidssøkere og permitterte som er helt eller delvis ledige, med sikte på at tiltaksdeltakere skaffer seg lønnet arbeid.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (30, 'Kronisk syk arbeidstaker (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (31, 'Kvalifiseringsprogrammet KVP', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (32, 'Lese- og sekretærhjelp (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (33, 'Reisetilskudd som alternativ til sykepenger (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (34, 'Servicerutine for reisetilskudd',
        '',
        'Det kan ytes reisetilskudd istedenfor sykepenger når en person midlertidig ikke kan reise til og fra arbeidsstedet på vanlig måte på grunn av sykdom eller skade.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (35, 'Servicehund (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (36, 'Sommerjobb (tilskudd)',
        '',
        'Arbeidsmarkedstiltaket sommerjobb skal bidra til at unge arbeidssøkere med behov for arbeidsrettet bistand kan få arbeidserfaring i form av en fire ukers sommerjobb.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (37, 'Tilskudd til sommerjobb',
        '',
        'Arbeidsmarkedstiltaket sommerjobb skal bidra til at unge arbeidssøkere med behov for arbeidsrettet bistand kan få arbeidserfaring i form av en fire ukers sommerjobb.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (38, 'Tilretteleggings. og oppfølgingsavtale',
        '',
        'Avtalen skal sikre at begge parter får regelmessig oppfølging, koordinert bistand og fast kontaktperson i NAV. Den skal bidra til at personer med nedsatt arbeidsevne kommer i - eller beholder- ordinært arbeid.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (39, 'Tilrettelegging i arbeid og utdanning', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (40, 'Tolkehjelp for hørselshemmede (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (41, 'Unntak fra arbeidsgiverperioden ved langvarig eller kronisk sykdom (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (42, 'Unntak fra arbeidsgiverperioden ved svangerskapsrelatert sykdom (nav.no)', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (43, 'Utvidet oppfølging i NAV', '', '<mangler>')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

INSERT INTO tiltaksvariant
VALUES (44, 'Varig tilrettelagt arbeid i ordinær bedrift (VTO)',
        '',
        'Varig tilrettelagt arbeid i ordinær virksomhet skal gi brukeren arbeid med oppgaver tilpasset den enkeltes arbeidsevne.')
ON CONFLICT (id) DO UPDATE SET tittel      = EXCLUDED.tittel,
                               ingress     = EXCLUDED.ingress,
                               beskrivelse = EXCLUDED.beskrivelse;

SELECT setval('tiltaksvariant_id_seq', (SELECT MAX(id) from "tiltaksvariant"));