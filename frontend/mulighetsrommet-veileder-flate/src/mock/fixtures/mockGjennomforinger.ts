import {
  GjennomforingOppstartstype,
  GjennomforingStatus,
  VeilederflateTiltak,
  VeilederflateTiltakEnkeltplass,
  VeilederflateTiltakGruppe,
} from "@api-client";
import { mockTiltakstyper } from "./mockTiltakstyper";

export const tiltakAvklaring: VeilederflateTiltak & VeilederflateTiltakGruppe = {
  type: "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppe",
  tiltaksnummer: "123123",
  id: "f4cea25b-c372-4d4c-8106-535ab10cd586",
  oppstartsdato: "2024-01-01",
  navn: "Avklaring - Fredrikstad med ganske langt navn som strekker seg bortover",
  status: {
    type: GjennomforingStatus.GJENNOMFORES,
    beskrivelse: "Gjennomføres",
  },
  oppstart: GjennomforingOppstartstype.LOPENDE,
  estimertVentetid: {
    verdi: 3,
    enhet: "maned",
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
    organisasjonsnummer: null,
  },
  fylker: ["0200"],
  enheter: [],
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Sindre",
        telefon: "12345678",
        enhet: {
          enhetsnummer: "1928",
          navn: "Nav Oslo",
        },
        epost: "test@example.com",
        beskrivelse: null,
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
    forHvemInfoboks: null,
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
    kontaktinfoInfoboks: null,
    detaljerOgInnhold: null,
    detaljerOgInnholdInfoboks: null,
    oppskrift: null,
    pameldingOgVarighet: null,
    pameldingOgVarighetInfoboks: null,
    delMedBruker: null,
  },
  beskrivelse: null,
};

export const tiltakMentor: VeilederflateTiltak & VeilederflateTiltakEnkeltplass = {
  type: "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplass",
  sanityId: "91205ff2-ec72-4a7f-80b8-1c99d8535a06",
  navn: "Sindres mentorordning med Yoda",
  oppstart: GjennomforingOppstartstype.LOPENDE,
  stedForGjennomforing: "Oslo",
  tiltakstype: mockTiltakstyper.mentor,
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Sindre",
        telefon: "12345678",
        enhet: {
          enhetsnummer: "1928",
          navn: "Nav Oslo",
        },
        epost: "test@example.com",
        beskrivelse: null,
      },
    ],
  },
  fylker: [],
  enheter: [],
  beskrivelse: null,
  faneinnhold: null,
};

export const tiltakAmoGruppe: VeilederflateTiltak & VeilederflateTiltakGruppe = {
  type: "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppe",
  id: "00097090-1ba8-47a4-a82f-6aaad488994e",
  status: {
    type: GjennomforingStatus.GJENNOMFORES,
    beskrivelse: "Gjennomføres",
  },
  personvernBekreftet: true,
  personopplysningerSomKanBehandles: [],
  tiltaksnummer: "2024/123",
  navn: "Kurs i overlevelsesteknikk",
  stedForGjennomforing: "2050 JESSHEIM",
  apentForPamelding: true,
  tiltakstype: mockTiltakstyper.gruppe_amo,
  oppstart: GjennomforingOppstartstype.FELLES,
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
    organisasjonsnummer: null,
  },
  fylker: ["0800"],
  enheter: [],
  beskrivelse:
    "Beskrivelse av gruppe amo tiltaket på gjennomføringsnivå. Her kommer det masse spennende informasjon om tiltaket.",
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefon: "48123456",
        enhet: {
          enhetsnummer: "1928",
          navn: "Nav Oslo",
        },
        epost: "pelle.pilotbruker@nav.no",
        beskrivelse: null,
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
    kontaktinfo: null,
    kontaktinfoInfoboks: null,
    detaljerOgInnholdInfoboks: null,
    oppskrift: null,
    pameldingOgVarighetInfoboks: null,
    delMedBruker: null,
  },
  estimertVentetid: null,
};

export const tiltakVta: VeilederflateTiltak & VeilederflateTiltakGruppe = {
  type: "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppe",
  id: "3b597090-1ba8-47a4-a82f-6aaad488994e",
  navn: "VTA hos Fretex",
  status: {
    type: GjennomforingStatus.GJENNOMFORES,
    beskrivelse: "Gjennomføres",
  },
  stedForGjennomforing: "2050",
  apentForPamelding: true,
  tiltakstype: mockTiltakstyper.VTA,
  oppstart: GjennomforingOppstartstype.LOPENDE,
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
    organisasjonsnummer: null,
  },
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefon: "48123456",
        enhet: {
          enhetsnummer: "1928",
          navn: "Nav Oslo",
        },
        epost: "pelle.pilotbruker@nav.no",
        beskrivelse: null,
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
    forHvemInfoboks: null,
    kontaktinfo: null,
    kontaktinfoInfoboks: null,
    detaljerOgInnholdInfoboks: null,
    oppskrift: null,
    pameldingOgVarighetInfoboks: null,
    delMedBruker: null,
    lenker: null,
  },
  fylker: [],
  enheter: [],
  beskrivelse: null,
  tiltaksnummer: null,
  estimertVentetid: null,
};

export const tiltakJobbklubb: VeilederflateTiltak & VeilederflateTiltakGruppe = {
  type: "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppe",
  id: "ff887090-1ba8-47a4-a82f-6aaad488994e",
  navn: "Jobbklubb (med Lars Monsen)",
  status: {
    type: GjennomforingStatus.GJENNOMFORES,
    beskrivelse: "Gjennomføres",
  },
  stedForGjennomforing: "Kautokeino",
  apentForPamelding: true,
  tiltakstype: mockTiltakstyper.jobbklubb,
  oppstart: GjennomforingOppstartstype.LOPENDE,
  oppstartsdato: "2022-11-01",
  sluttdato: "2030-11-30",
  personvernBekreftet: true,
  personopplysningerSomKanBehandles: [],
  arrangor: {
    selskapsnavn: "LARS MONSEN AS AVD FINNMARK",
    kontaktpersoner: [],
    organisasjonsnummer: null,
  },
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefon: "48123456",
        enhet: {
          enhetsnummer: "1928",
          navn: "Nav Oslo",
        },
        epost: "pelle.pilotbruker@nav.no",
        beskrivelse: null,
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
    forHvemInfoboks: null,
    kontaktinfo: null,
    kontaktinfoInfoboks: null,
    detaljerOgInnhold: null,
    detaljerOgInnholdInfoboks: null,
    oppskrift: null,
    pameldingOgVarighet: null,
    pameldingOgVarighetInfoboks: null,
    delMedBruker: null,
    lenker: null,
  },
  fylker: [],
  enheter: [],
  beskrivelse: null,
  tiltaksnummer: null,
  estimertVentetid: null,
};

export const tiltakAft: VeilederflateTiltak & VeilederflateTiltakGruppe = {
  type: "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppe",
  id: "bdfa7090-1ba8-47a4-a82f-6aaad488994e",
  navn: "AFT",
  stedForGjennomforing: "Sinsen",
  status: {
    type: GjennomforingStatus.GJENNOMFORES,
    beskrivelse: "Gjennomføres",
  },
  apentForPamelding: true,
  tiltakstype: mockTiltakstyper.AFT,
  oppstart: GjennomforingOppstartstype.LOPENDE,
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
    organisasjonsnummer: null,
  },
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefon: "48123456",
        enhet: {
          enhetsnummer: "1928",
          navn: "Nav Oslo",
        },
        epost: "pelle.pilotbruker@nav.no",
        beskrivelse: null,
      },
    ],
  },
  fylker: [],
  enheter: [],
  beskrivelse: null,
  faneinnhold: null,
  tiltaksnummer: null,
  estimertVentetid: null,
};

export const tiltakOppfolging: VeilederflateTiltak & VeilederflateTiltakGruppe = {
  type: "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppe",
  id: "3c110e8c-5867-4ece-b343-e9b1c547f548",
  navn: "Oppfølging",
  stedForGjennomforing: "Sinsen",
  status: {
    type: GjennomforingStatus.GJENNOMFORES,
    beskrivelse: "Gjennomføres",
  },
  apentForPamelding: true,
  tiltakstype: mockTiltakstyper.oppfolging,
  oppstart: GjennomforingOppstartstype.LOPENDE,
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
    organisasjonsnummer: null,
  },
  kontaktinfo: {
    tiltaksansvarlige: [],
  },
  fylker: [],
  enheter: [],
  beskrivelse: null,
  faneinnhold: null,
  tiltaksnummer: null,
  estimertVentetid: null,
};

export const tiltakEnkelplassFagOgYrke: VeilederflateTiltak & VeilederflateTiltakEnkeltplass = {
  type: "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplass",
  sanityId: "f1887090-1ba8-47a4-a82f-6aaad488994e",
  navn: "Opplæring Fag og Yrke",
  stedForGjennomforing: "Oslo",
  tiltakstype: mockTiltakstyper.OpplaringEnkeltplassFagOgYrke,
  oppstart: GjennomforingOppstartstype.LOPENDE,
  kontaktinfo: {
    tiltaksansvarlige: [
      {
        navn: "Truls Svendsen",
        epost: "test@example.com",
        telefon: null,
        enhet: null,
        beskrivelse: null,
      },
    ],
  },
  fylker: [],
  enheter: [],
  beskrivelse: null,
  faneinnhold: null,
};

export const mockGjennomforinger: VeilederflateTiltak[] = [
  tiltakAvklaring,
  tiltakMentor,
  tiltakAmoGruppe,
  tiltakVta,
  tiltakJobbklubb,
  tiltakAft,
  tiltakEnkelplassFagOgYrke,
];
