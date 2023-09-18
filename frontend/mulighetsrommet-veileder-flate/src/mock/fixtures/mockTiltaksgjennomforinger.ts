import {
  Innsatsgruppe,
  Tilgjengelighetsstatus,
  TiltaksgjennomforingOppstartstype,
  VeilederflateInnsatsgruppe,
  VeilederflateTiltaksgjennomforing,
  VeilederflateTiltakstype,
} from 'mulighetsrommet-api-client';

export const mockTiltaksgjennomforinger: VeilederflateTiltaksgjennomforing[] = [
  {
    sanityId: 'f4cea25b-c372-4d4c-8106-535ab10cd586',
    navn: 'Avklaring - Fredrikstad',
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    lokasjon: '1424 SKI',
    tiltakstype: {
      sanityId: 'f9618e97-4510-49e2-b748-29cae84d9019',
      beskrivelse:
        'Avklaring skal kartlegge deltakerens muligheter for arbeid og behov for bistand til å skaffe eller beholde arbeid. Avklaringen skal bidra til at deltaker får økt innsikt i sine muligheter på arbeidsmarkedet og i egne ressurser og ferdigheter i jobbsammenheng.',
      regelverkLenker: [
        {
          _id: 'd5c37609-2b4d-4985-8093-703e198385c1',
          regelverkUrl: 'https://lovdata.no/dokument/SF/forskrift/2015-12-11-1598#KAPITTEL_7',
          regelverkLenkeNavn: 'Forskrift',
        },
        {
          _id: '799ee31e-ad3c-4c4d-9efd-a6fc71ca959d',
          regelverkUrl: 'https://lovdata.no/nav/rundskriv/r76-12-01#KAPITTEL_8',
          regelverkLenkeNavn: 'Rundskriv',
        },
      ],
      innsatsgruppe: {
        sanityId: '48a20a99-11d7-42ec-ba92-2245b7d88fa7',
        beskrivelse: 'Situasjonsbestemt innsats',
        nokkel: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        tittel: VeilederflateInnsatsgruppe.tittel.SITUASJONSBESTEMT_INNSATS,
        order: 1,
      },
      navn: 'Avklaring',
      faneinnhold: {
        forHvemInfoboks: 'En helt spesiell infoboks som viser veldig viktig informasjon',
        forHvem: [
          {
            style: 'normal',
            _key: '8caa6fa462f4',
            _type: 'block',
            children: [
              {
                text: 'Hvem kan delta:',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: '799b5d1b6883',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Personer som har behov for kartlegging av fremtidig arbeidsmuligheter',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: '6fa666901dca',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Åpen for brukere fra alle innsatsgrupper unntatt brukere med standard innsats',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: 'c6a9d5736bad',
            _type: 'block',
            children: [
              {
                text: 'For sykmeldte arbeidstakere er det ikke nødvendig med behovs-/arbeidsevnevurdering og 14a-vedtak.\n',
                _type: 'span',
              },
            ],
          },
        ],
        detaljerOgInnhold: [
          {
            style: 'normal',
            _key: '020afe77d2d8',
            _type: 'block',
            children: [
              {
                text: 'Kurset inneholder:',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: 'bf909fd8a322',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Kartlegge hvordan faglig og sosial kompetanse, helse, ferdigheter, evner og interesser samt andre forhold påvirker muligheten for å skaffe eller beholde arbeid',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: 'ec499aa64d19',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Kartlegging av hvilken type tilrettelegging og individuell bistand deltaker trenger for å stå i et arbeid',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: '9d3672c598c3',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Kartlegging av basiskompetanse/grunnleggende ferdigheter',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: 'f7467a7d69f7',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Informasjon og økt kunnskap om arbeidsmarked, yrker og jobbkrav',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: 'ca22c21c2aa4',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Karriereveiledning for valg av realistiske yrkesmål og arbeidsoppgaver',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: '8a1916f627f7',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Motivasjons- og mestringsaktiviteter som skal bidra til at deltaker utarbeider egne målsetninger for å kunne skaffe eller beholde arbeid',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: '5995993c0a33',
            _type: 'block',
            children: [
              {
                text: 'Tiltaket skal ikke benyttes for å kartlegge behovet til bruker for full uførepensjon. ',
                _type: 'span',
              },
            ],
          },
        ],
        pameldingOgVarighet: [
          {
            style: 'normal',
            _key: '6e17ce995ef3',
            _type: 'block',
            children: [
              {
                text: 'Avklaring kan vare i inntil 4 uker, men med mulighet til forlengelse i ytterligere inntil 8 uker etter en nærmere vurdering ved særlige behov.',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: 'd3b0d033665d',
            _type: 'block',
            children: [
              {
                text: '\nTiltaket gjennomføres som et heltidstilbud med 30 timer pr uke, 5 virkedager pr uke. ',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: 'ea54a5378f03',
            _type: 'block',
            children: [
              {
                text: '',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: 'd8271280b026',
            _type: 'block',
            children: [
              {
                text: 'Omfanget skal være individuelt tilpasset til den enkelte deltakers behov.',
                _type: 'span',
              },
            ],
          },
        ],
      },
      delingMedBruker:
        'Hei <Fornavn>, \n\nVi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn>.\n\nI tiltaket kan det være aktuelt å kartlegge og gi hjelp til å\n- tilpasse arbeidssituasjonen og -oppgaver slik at du kan utføre jobben\n- finne ut hva slags hjelp eller tilrettelegging som skal til for at du kan jobbe\n- se kompetansen din og mulighetene dine\n\nUnder avklaring kan du også få\n- informasjon om arbeidsmarkedet, yrker og jobbkrav\n- veiledning for å velge yrkesmål og arbeidsoppgaver\n- arbeidsutprøving på en arbeidsplass\n\nDu kan lese mer om kurset på www.nav.no/avklaring ',
    },
    sluttdato: '2025-07-09',
    kontaktinfoTiltaksansvarlige: [
      {
        navn: 'Sindre',
        telefonnummer: '12345678',
        _id: '8ea86d71-4c1b-4c8f-81b5-49ec67ef1d17',
        enhet: '123456',
        epost: 'test@example.com',
      },
    ],
  },
  {
    sanityId: '91205ff2-ec72-4a7f-80b8-1c99d8535a06',
    navn: 'Sindres mentorordning med Yoda',
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    lokasjon: 'Oslo',
    tiltakstype: {
      sanityId: 'ad998fc6-310e-45d4-a056-57732fed87b4',
      beskrivelse:
        'Mentor skal gi nødvendig bistand til å kunne gjennomføre arbeidsmarkedstiltak, eller for å kunne få eller beholde lønnet arbeid i en ordinær bedrift.',
      regelverkLenker: [
        {
          _id: 'd5c37609-2b4d-4985-8093-703e198385c1',
          regelverkUrl: 'https://lovdata.no/dokument/SF/forskrift/2015-12-11-1598#KAPITTEL_7',
          regelverkLenkeNavn: 'Forskrift',
        },
        {
          _id: '799ee31e-ad3c-4c4d-9efd-a6fc71ca959d',
          regelverkUrl: 'https://lovdata.no/nav/rundskriv/r76-12-01#KAPITTEL_8',
          regelverkLenkeNavn: 'Rundskriv',
        },
      ],
      innsatsgruppe: {
        sanityId: '48a20a99-11d7-42ec-ba92-2245b7d88fa7',
        beskrivelse: 'Situasjonsbestemt innsats',
        nokkel: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        tittel: VeilederflateInnsatsgruppe.tittel.SITUASJONSBESTEMT_INNSATS,
        order: 1,
      },
      navn: 'Mentor',
      faneinnhold: {
        forHvem: [
          {
            style: 'normal',
            _key: '6691c5e92f46',
            _type: 'block',
            children: [
              {
                text: 'Personer som trenger støtte for å kunne gjennomføre tiltak i form av arbeidstrening, opplæring ved ordinær utdanning, sommerjobb, midlertidig lønnstilskudd eller varig lønnstilskudd. ',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: '5db2a6cfb6c1',
            _type: 'block',
            children: [
              {
                text: 'Tilskudd til mentor kan i tillegg gis hvis det er nødvendig for å kunne få eller beholde lønnet arbeid i en ordinær virksomhet.',
                _type: 'span',
              },
            ],
          },
        ],
        detaljerOgInnhold: [
          {
            style: 'normal',
            _key: '0619db5c5582',
            _type: 'block',
            children: [
              {
                text: 'En mentor er en frikjøpt kollega eller en medstudent, som kan gi praktisk hjelp, veiledning og opplæring, tilpasset den enkeltes individuelle behov',
                _type: 'span',
              },
            ],
          },
        ],
        pameldingOgVarighet: [
          {
            style: 'normal',
            _key: 'bfa7ef914b12',
            _type: 'block',
            children: [
              {
                text: 'Påmelding til mentor er tilgjengelig som digital avtale. Det vil si at deltaker, arbeidsgiver og NAV fyller ut, ser over og godkjenner avtalen i samme løsning i sanntid. Mentoren er ikke en part i avtalen, men har lesetilgang til det meste i avtalen og må signere en taushetserklæring.',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: '69a1e2b531a4',
            _type: 'block',
            children: [
              {
                text: 'Les mer om hvordan digital avtale fungerer',
                _type: 'span',
              },
            ],
          },
        ],
      },
      arenakode: VeilederflateTiltakstype.arenakode.MENTOR,
    },
    kontaktinfoTiltaksansvarlige: [
      {
        navn: 'Sindre',
        telefonnummer: '12345678',
        _id: '8ea86d71-4c1b-4c8f-81b5-49ec67ef1d17',
        enhet: '123456',
        epost: 'test@example.com',
      },
    ],
  },
  {
    sanityId: '2a597090-1ba8-47a4-a82f-6aaad488994e',
    navn: 'Kurs i overlevelsesteknikk med Lars Monsen',
    lokasjon: '2050 JESSHEIM',
    tilgjengelighet: Tilgjengelighetsstatus.LEDIG,
    tiltakstype: {
      sanityId: 'eadeb22c-bd89-4298-a5c2-145f112f8e7d',
      beskrivelse:
        'Opplæringstiltak i form av Gruppe AMO (jf. tiltaksforskriften § 7-2 a) skal bidra til at arbeidssøkere kvalifiseres til ledige jobber. ',
      regelverkLenker: [
        {
          _id: 'd5c37609-2b4d-4985-8093-703e198385c1',
          regelverkUrl: 'https://lovdata.no/dokument/SF/forskrift/2015-12-11-1598#KAPITTEL_7',
          regelverkLenkeNavn: 'Forskrift',
        },
        {
          _id: '799ee31e-ad3c-4c4d-9efd-a6fc71ca959d',
          regelverkUrl: 'https://lovdata.no/nav/rundskriv/r76-12-01#KAPITTEL_8',
          regelverkLenkeNavn: 'Rundskriv',
        },
      ],
      innsatsgruppe: {
        sanityId: '48a20a99-11d7-42ec-ba92-2245b7d88fa7',
        beskrivelse: 'Situasjonsbestemt innsats',
        nokkel: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        tittel: VeilederflateInnsatsgruppe.tittel.SITUASJONSBESTEMT_INNSATS,
        order: 1,
      },
      navn: 'Opplæring - Gruppe AMO',
      faneinnhold: {
        forHvem: [
          {
            style: 'normal',
            _key: '965337f85d9a',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Brukere med behov for kvalifisering for å komme inn på arbeidsmarkedet',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: '3c9d2cdb5a6a',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Brukere kan starte i AMO det året de fyller 19 år',
                _type: 'span',
              },
            ],
          },
          {
            style: 'normal',
            _key: 'a05232acf53d',
            level: 1,
            listItem: 'bullet',
            _type: 'block',
            children: [
              {
                text: 'Åpent for brukere med situasjonsbestemt, spesielt tilpasset og varig tilpasset innsats',
                _type: 'span',
              },
            ],
          },
        ],
        detaljerOgInnhold: [
          {
            style: 'normal',
            _key: 'f6710efeab29',
            _type: 'block',
            children: [
              {
                text: 'Gruppe AMO er for de som er glad i grupper',
                _type: 'span',
              },
            ],
          },
        ],
        pameldingOgVarighet: [
          {
            style: 'normal',
            _key: '4a8d530e6655',
            _type: 'block',
            children: [
              {
                text: 'Varighet er spesifisert per gjennomføring',
                _type: 'span',
              },
            ],
          },
        ],
      },
      delingMedBruker:
        'Hei <Fornavn>,   \n\nHar du vurdert utdanning for å få flere muligheter på arbeidsmarkedet?  \n\nDu kan lese mer om tiltaket på www.nav.no/opplaring \n\nEr dette aktuelt for deg? Gi meg tilbakemelding her i dialogen. \nSvaret ditt vil ikke endre din utbetaling fra NAV. \n\nVi holder kontakten!\nHilsen <Veiledernavn> \n',
      arenakode: VeilederflateTiltakstype.arenakode.GRUPPEAMO,
    },
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    oppstartsdato: '2023-11-01',
    sluttdato: '2023-11-30',
    arrangor: {
      selskapsnavn: 'JOBLEARN AS AVD 813201 ØST-VIKEN KURS',
      lokasjon: '2050 JESSHEIM',
      kontaktperson: {
        navn: 'Ole Testesen',
        telefon: '12345678',
        epost: 'test@example.com',
      },
    },
    estimertVentetid: '',
    kontaktinfoTiltaksansvarlige: [
      {
        navn: 'Pelle Pilotbruker',
        telefonnummer: '48123456',
        enhet: '1928',
        epost: 'pelle.pilotbruker@nav.no',
        _id: '56767',
      },
    ],
  },
];
