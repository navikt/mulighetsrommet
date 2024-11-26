import {
  EstimertVentetidEnhet,
  NavEnhetStatus,
  NavEnhetType,
  TiltaksgjennomforingOppstartstype,
  TiltaksgjennomforingStatus,
  VeilederflateTiltak,
  VeilederflateTiltakEnkeltplass,
  VeilederflateTiltakGruppe,
} from "@mr/api-client";
import { mockTiltakstyper } from "./mockTiltakstyper";

export const tiltakAvklaring: VeilederflateTiltakGruppe = {
  type: "TILTAK_GRUPPE",
  tiltaksnummer: "123123",
  id: "f4cea25b-c372-4d4c-8106-535ab10cd586",
  oppstartsdato: "2024-01-01",
  tittel: "Avklaring - Fredrikstad med ganske langt navn som strekker seg bortover",
  underTittel: "Avklaring",
  status: TiltaksgjennomforingStatus.GJENNOMFORES,
  oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
  estimertVentetid: {
    verdi: 3,
    enhet: EstimertVentetidEnhet.MANED,
  },
  stedForGjennomforing:
    "Valpekullsveien 69, 1424" +
    " Taumatawhakatangi­hangakoauauotamatea­turipukakapikimaunga­horonukupokaiwhen­uakitanatahu",
  tiltakstype: mockTiltakstyper.avklaring,
  sluttdato: "2025-07-09",
  apentForPamelding: true,
  personvernBekreftet: false,
  personopplysningerSomKanBehandles: [],
  arrangor: {
    selskapsnavn: "JOBLEARN AS AVD 813201 ØST-VIKEN KURS",
    kontaktpersoner: [
      {
        id: "1",
        navn: "Ole Testesen",
        telefon: "12345678",
        epost: "test@example.com",
        beskrivelse: null,
      },
    ],
  },
  fylke: "0200",
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Sindre",
        telefon: "12345678",
        _id: "8ea86d71-4c1b-4c8f-81b5-49ec67ef1d17",
        enhet: {
          enhetsnummer: "1928",
          navn: "Nav Oslo",
          type: NavEnhetType.LOKAL,
          status: NavEnhetStatus.AKTIV,
          overordnetEnhet: null,
        },
        epost: "test@example.com",
      },
    ],
  },
  faneinnhold: {
    lenker: [
      {
        lenke: "https://www.google.com",
        lenkenavn: "Søk via Google",
        apneINyFane: true,
        visKunForVeileder: false,
      },
      {
        lenke: "https://www.vg.no",
        lenkenavn: "Sjekk nyhetene på VG",
        apneINyFane: false,
        visKunForVeileder: true,
      },
    ],
    forHvem: [
      {
        style: "normal",
        children: [
          {
            text: "Tiltaket er for deltakere som er synshemmet eller døve/hørselshemmet.",
            _type: "span",
          },
        ],
        _type: "block",
      },
    ],
    kontaktinfo: [
      {
        style: "normal",
        children: [
          {
            text: "Oppmøtested er Andeby 1, 0669 Donald-Pocketby",
            _type: "span",
          },
        ],
        _type: "block",
      },
      {
        style: "normal",
        children: [
          {
            text: "Postadresse er Fyrstikkalleen 1, 0101 Oslo",
            _type: "span",
          },
        ],
        _type: "block",
      },
    ],
  },
};

export const tiltakMentor: VeilederflateTiltakEnkeltplass = {
  type: "TILTAK_ENKELTPLASS",
  sanityId: "91205ff2-ec72-4a7f-80b8-1c99d8535a06",
  tittel: "Mentor",
  underTittel: "Sindres mentorordning med Yoda",
  status: TiltaksgjennomforingStatus.GJENNOMFORES,
  oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
  stedForGjennomforing: "Oslo",
  tiltakstype: mockTiltakstyper.mentor,
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Sindre",
        telefon: "12345678",
        _id: "8ea86d71-4c1b-4c8f-81b5-49ec67ef1d17",
        enhet: {
          enhetsnummer: "1928",
          status: NavEnhetStatus.AKTIV,
          navn: "Nav Oslo",
          type: NavEnhetType.LOKAL,
          overordnetEnhet: null,
        },
        epost: "test@example.com",
      },
    ],
  },
};

export const tiltakAmoGruppe: VeilederflateTiltakGruppe = {
  type: "TILTAK_GRUPPE",
  id: "00097090-1ba8-47a4-a82f-6aaad488994e",
  status: TiltaksgjennomforingStatus.GJENNOMFORES,
  personvernBekreftet: true,
  personopplysningerSomKanBehandles: [],
  tiltaksnummer: "2024/123",
  tittel: "Kurs i overlevelsesteknikk",
  underTittel: "Gruppe AMO",
  stedForGjennomforing: "2050 JESSHEIM",
  apentForPamelding: true,
  tiltakstype: mockTiltakstyper.gruppe_amo,
  oppstart: TiltaksgjennomforingOppstartstype.FELLES,
  oppstartsdato: "2023-11-01",
  sluttdato: "2023-11-30",
  arrangor: {
    selskapsnavn: "JOBLEARN AS AVD 813201 ØST-VIKEN KURS",
    kontaktpersoner: [
      {
        id: "1",
        navn: "Ole Testesen",
        telefon: "12345678",
        epost: "test@example.com",
        beskrivelse: null,
      },
    ],
  },
  fylke: "0800",
  beskrivelse: "Beskrivelse av gruppe amo tiltaket på gjennomføringsnivå.",
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefon: "48123456",
        enhet: {
          status: NavEnhetStatus.AKTIV,
          enhetsnummer: "1928",
          navn: "Nav Oslo",
          type: NavEnhetType.LOKAL,
          overordnetEnhet: null,
        },
        epost: "pelle.pilotbruker@nav.no",
        _id: "56767",
      },
    ],
  },
  faneinnhold: {
    kurstittel: "Dette er en kurstittel for gruppe amo",
    lenker: [
      {
        lenke: "https://www.google.com",
        lenkenavn: "Søk via Google",
        apneINyFane: true,
        visKunForVeileder: false,
      },
      {
        lenke: "https://www.vg.no",
        lenkenavn: "Sjekk nyhetene på VG",
        apneINyFane: false,
        visKunForVeileder: true,
      },
    ],
    forHvemInfoboks: "Deltakeren må være forberedt på dårlig vær under tiltaksperioden",
    forHvem: [
      {
        style: "normal",
        children: [
          {
            text: "Kurset holdes i Østmarka og vil foregå over en tre ukers periode. Deltaker bør ikke gå på spesielle medisiner, være skadet eller ha funksjonsnedsettelser i forbindelse med forflytting.",
            _type: "span",
          },
        ],
        _type: "block",
      },
      {
        _type: "block",
        listItem: "bullet",
        markDefs: [{ _type: "link", _key: "nav.no", href: "https://nav.no" }],
        children: [
          { _type: "span", text: "Du kan lese mer om tiltaket på " },
          { _type: "span", text: "Nav.no ", marks: ["nav.no"] },
        ],
      },
    ],
    detaljerOgInnhold: [
      {
        style: "normal",
        children: [
          {
            text: "Virksomheten skal gi tilpassede oppgaver etter den ansattes ønsker, behov og forutsetninger.",
            _type: "span",
          },
        ],
        _type: "block",
      },
    ],
    pameldingOgVarighet: [
      {
        style: "normal",
        children: [
          {
            text: "Ta kontakt med Nav-kontoret der du bor. Nav vurderer sammen med deg om du har behov for tiltaket. Nav avgjør om du får tilbudet.",
            _type: "span",
          },
        ],
        _type: "block",
      },
    ],
  },
};

export const tiltakVta: VeilederflateTiltakGruppe = {
  type: "TILTAK_GRUPPE",
  id: "3b597090-1ba8-47a4-a82f-6aaad488994e",
  underTittel: "VTA hos Fretex",
  tittel: "VTA",
  status: TiltaksgjennomforingStatus.GJENNOMFORES,
  stedForGjennomforing: "2050",
  apentForPamelding: true,
  tiltakstype: mockTiltakstyper.VTA,
  oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
  oppstartsdato: "2023-11-01",
  sluttdato: "2023-11-30",
  personvernBekreftet: true,
  personopplysningerSomKanBehandles: [],
  arrangor: {
    selskapsnavn: "FRETEX",
    kontaktpersoner: [
      {
        id: "1",
        navn: "Ole Testesen",
        telefon: "12345678",
        epost: "test@example.com",
        beskrivelse: null,
      },
    ],
  },
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefon: "48123456",
        enhet: {
          enhetsnummer: "1928",
          status: NavEnhetStatus.AKTIV,
          navn: "Nav Oslo",
          type: NavEnhetType.LOKAL,
          overordnetEnhet: null,
        },
        epost: "pelle.pilotbruker@nav.no",
        _id: "56767",
      },
    ],
  },
  faneinnhold: {
    forHvem: [
      {
        style: "normal",
        children: [
          {
            text: "Tilbudet kan være aktuelt for deg som har uføretrygd, eller i nær framtid forventer å få uføretrygd, og trenger spesiell tilrettelegging og oppfølging.",
            _type: "span",
          },
        ],
        _type: "block",
      },
      {
        children: [
          {
            _type: "span",
            text: "Når du deltar på tiltaket varig tilrettelagt arbeid, får du en arbeidskontrakt på lik linje med andre arbeidstakere etter arbeidsmiljøloven.",
          },
        ],
        _type: "block",
        style: "normal",
      },
    ],
    detaljerOgInnhold: [
      {
        style: "normal",
        children: [
          {
            text: "Virksomheten skal gi tilpassede oppgaver etter den ansattes ønsker, behov og forutsetninger.",
            _type: "span",
          },
        ],
        _type: "block",
      },
    ],
    pameldingOgVarighet: [
      {
        style: "normal",
        children: [
          {
            text: "Ta kontakt med Nav-kontoret der du bor. Nav vurderer sammen med deg om du har behov for tiltaket. Nav avgjør om du får tilbudet.",
            _type: "span",
          },
        ],
        _type: "block",
      },
    ],
  },
};

export const tiltakJobbklubb: VeilederflateTiltakGruppe = {
  type: "TILTAK_GRUPPE",
  id: "ff887090-1ba8-47a4-a82f-6aaad488994e",
  tittel: "Jobbklubb (med Lars Monsen)",
  underTittel: "Jobbklubb",
  status: TiltaksgjennomforingStatus.GJENNOMFORES,
  stedForGjennomforing: "Kautokeino",
  apentForPamelding: true,
  tiltakstype: mockTiltakstyper.jobbklubb,
  oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
  oppstartsdato: "2022-11-01",
  sluttdato: "2030-11-30",
  personvernBekreftet: true,
  personopplysningerSomKanBehandles: [],
  arrangor: {
    selskapsnavn: "LARS MONSEN AS AVD FINNMARK",
    kontaktpersoner: [],
  },
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefon: "48123456",
        enhet: {
          enhetsnummer: "1928",
          navn: "Nav Oslo",
          type: NavEnhetType.LOKAL,
          overordnetEnhet: null,
          status: NavEnhetStatus.AKTIV,
        },
        epost: "pelle.pilotbruker@nav.no",
        _id: "56767",
      },
    ],
  },
  faneinnhold: {
    forHvem: [
      {
        style: "normal",
        children: [
          {
            text: "Kurset er for personer med høyere utdanning.",
            _type: "span",
          },
        ],
        _type: "block",
      },
    ],
  },
};

export const tiltakAft: VeilederflateTiltakGruppe = {
  type: "TILTAK_GRUPPE",
  id: "bdfa7090-1ba8-47a4-a82f-6aaad488994e",
  tittel: "AFT",
  underTittel: "AFT",
  stedForGjennomforing: "Sinsen",
  status: TiltaksgjennomforingStatus.GJENNOMFORES,
  apentForPamelding: true,
  tiltakstype: mockTiltakstyper.AFT,
  oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
  oppstartsdato: "2022-11-01",
  sluttdato: "2030-11-30",
  personvernBekreftet: true,
  personopplysningerSomKanBehandles: [],
  arrangor: {
    selskapsnavn: "AFT GRUPPEN NORWAY",
    kontaktpersoner: [
      {
        id: "1",
        navn: "Ole Testesen",
        telefon: "12345678",
        epost: "test@example.com",
        beskrivelse: null,
      },
    ],
  },
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefon: "48123456",
        enhet: {
          enhetsnummer: "1928",
          navn: "Nav Oslo",
          type: NavEnhetType.LOKAL,
          overordnetEnhet: null,
          status: NavEnhetStatus.AKTIV,
        },
        epost: "pelle.pilotbruker@nav.no",
        _id: "56767",
      },
    ],
  },
};

export const tiltakOppfolging: VeilederflateTiltakGruppe = {
  type: "TILTAK_GRUPPE",
  id: "3c110e8c-5867-4ece-b343-e9b1c547f548",
  tittel: "Oppfølging",
  underTittel: "Oppfølging",
  stedForGjennomforing: "Sinsen",
  status: TiltaksgjennomforingStatus.GJENNOMFORES,
  apentForPamelding: true,
  tiltakstype: mockTiltakstyper.oppfolging,
  oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
  oppstartsdato: "2022-11-01",
  sluttdato: "2030-11-30",
  personvernBekreftet: true,
  personopplysningerSomKanBehandles: [],
  arrangor: {
    selskapsnavn: "AFT GRUPPEN NORWAY",
    kontaktpersoner: [
      {
        id: "1",
        navn: "Ole Testesen",
        telefon: "12345678",
        epost: "test@example.com",
        beskrivelse: null,
      },
    ],
  },
  kontaktinfo: {
    tiltaksansvarlige: [],
  },
};

export const tiltakEnkelplassFagOgYrke: VeilederflateTiltakEnkeltplass = {
  type: "TILTAK_ENKELTPLASS",
  sanityId: "f1887090-1ba8-47a4-a82f-6aaad488994e",
  status: TiltaksgjennomforingStatus.GJENNOMFORES,
  tittel: "Opplæring Fag og Yrke",
  underTittel: "Opplæring Fag og Yrke",
  stedForGjennomforing: "Oslo",
  tiltakstype: mockTiltakstyper.OpplaringEnkeltplassFagOgYrke,
  oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        _id: "1",
        navn: "Truls Svendsen",
        epost: "test@example.com",
      },
    ],
  },
};

export const mockTiltaksgjennomforinger: VeilederflateTiltak[] = [
  tiltakAvklaring,
  tiltakMentor,
  tiltakAmoGruppe,
  tiltakVta,
  tiltakJobbklubb,
  tiltakAft,
  tiltakEnkelplassFagOgYrke,
];
