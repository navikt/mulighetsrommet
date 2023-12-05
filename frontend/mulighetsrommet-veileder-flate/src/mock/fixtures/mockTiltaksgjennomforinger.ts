import {
  TiltaksgjennomforingOppstartstype,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { mockTiltakstyper } from "./mockTiltakstyper";

export const mockTiltaksgjennomforinger: VeilederflateTiltaksgjennomforing[] = [
  {
    sanityId: "f4cea25b-c372-4d4c-8106-535ab10cd586",
    navn: "Avklaring - Fredrikstad",
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    stedForGjennomforing: "1424 SKI",
    tiltakstype: mockTiltakstyper.avklaring,
    sluttdato: "2025-07-09",
    apentForInnsok: true,
    kontaktinfoTiltaksansvarlige: [
      {
        navn: "Sindre",
        telefonnummer: "12345678",
        _id: "8ea86d71-4c1b-4c8f-81b5-49ec67ef1d17",
        enhet: "123456",
        epost: "test@example.com",
      },
    ],
    faneinnhold: {
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
    },
  },
  {
    sanityId: "91205ff2-ec72-4a7f-80b8-1c99d8535a06",
    navn: "Sindres mentorordning med Yoda",
    oppstart: TiltaksgjennomforingOppstartstype.FELLES,
    oppstartsdato: new Date().toDateString(),
    stedForGjennomforing: "Oslo",
    tiltakstype: mockTiltakstyper.mentor,
    apentForInnsok: false,
    kontaktinfoTiltaksansvarlige: [
      {
        navn: "Sindre",
        telefonnummer: "12345678",
        _id: "8ea86d71-4c1b-4c8f-81b5-49ec67ef1d17",
        enhet: "123456",
        epost: "test@example.com",
      },
    ],
  },
  {
    sanityId: "00097090-1ba8-47a4-a82f-6aaad488994e",
    navn: "Kurs i overlevelsesteknikk (Gruppe AMO)",
    stedForGjennomforing: "2050 JESSHEIM",
    apentForInnsok: true,
    tiltakstype: mockTiltakstyper.gruppe_amo,
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    oppstartsdato: "2023-11-01",
    sluttdato: "2023-11-30",
    arrangor: {
      selskapsnavn: "JOBLEARN AS AVD 813201 ØST-VIKEN KURS",
      kontaktperson: {
        navn: "Ole Testesen",
        telefon: "12345678",
        epost: "test@example.com",
      },
    },
    beskrivelse: "Beskrivelse av gruppe amo tiltaket på gjennomføringsnivå.",
    kontaktinfoTiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefonnummer: "48123456",
        enhet: "1928",
        epost: "pelle.pilotbruker@nav.no",
        _id: "56767",
      },
    ],
    faneinnhold: {
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
            { _type: "span", text: "NAV.no ", marks: ["nav.no"] },
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
              text: "Ta kontakt med NAV-kontoret der du bor. NAV vurderer sammen med deg om du har behov for tiltaket. NAV avgjør om du får tilbudet.",
              _type: "span",
            },
          ],
          _type: "block",
        },
      ],
    },
  },
  {
    sanityId: "3b597090-1ba8-47a4-a82f-6aaad488994e",
    navn: "VTA hos Fretex",
    stedForGjennomforing: "2050",
    apentForInnsok: true,
    tiltakstype: mockTiltakstyper.VTA,
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    oppstartsdato: "2023-11-01",
    sluttdato: "2023-11-30",
    arrangor: {
      selskapsnavn: "FRETEX",
      kontaktperson: {
        navn: "Ole Testesen",
        telefon: "12345678",
        epost: "test@example.com",
      },
    },
    kontaktinfoTiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefonnummer: "48123456",
        enhet: "1928",
        epost: "pelle.pilotbruker@nav.no",
        _id: "56767",
      },
    ],
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
              text: "Ta kontakt med NAV-kontoret der du bor. NAV vurderer sammen med deg om du har behov for tiltaket. NAV avgjør om du får tilbudet.",
              _type: "span",
            },
          ],
          _type: "block",
        },
      ],
    },
  },
  {
    sanityId: "ff887090-1ba8-47a4-a82f-6aaad488994e",
    navn: "Jobbklubb (med Lars Monsen)",
    stedForGjennomforing: "Kautokeino",
    apentForInnsok: true,
    tiltakstype: mockTiltakstyper.jobbklubb,
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    oppstartsdato: "2022-11-01",
    sluttdato: "2030-11-30",
    arrangor: {
      selskapsnavn: "LARS MONSEN AS AVD FINNMARK",
      kontaktperson: {
        navn: "Mona Monsen",
        telefon: "12345678",
        epost: "test@example.com",
      },
    },
    kontaktinfoTiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefonnummer: "48123456",
        enhet: "1928",
        epost: "pelle.pilotbruker@nav.no",
        _id: "56767",
      },
    ],
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
  },
  {
    sanityId: "bdfa7090-1ba8-47a4-a82f-6aaad488994e",
    navn: "AFT",
    stedForGjennomforing: "Sinsen",
    apentForInnsok: true,
    tiltakstype: mockTiltakstyper.AFT,
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    oppstartsdato: "2022-11-01",
    sluttdato: "2030-11-30",
    arrangor: {
      selskapsnavn: "AFT GRUPPEN NORWAY",
      kontaktperson: {
        navn: "Louise Gran",
        telefon: "12345678",
        epost: "test@example.com",
      },
    },
    kontaktinfoTiltaksansvarlige: [
      {
        navn: "Pelle Pilotbruker",
        telefonnummer: "48123456",
        enhet: "1928",
        epost: "pelle.pilotbruker@nav.no",
        _id: "56767",
      },
    ],
  },
  {
    sanityId: "f1887090-1ba8-47a4-a82f-6aaad488994e",
    navn: "Oppæring Fag og Yrke",
    stedForGjennomforing: "Oslo",
    apentForInnsok: true,
    tiltakstype: mockTiltakstyper.OpplaringEnkeltplassFagOgYrke,
    oppstart: TiltaksgjennomforingOppstartstype.LOPENDE,
    oppstartsdato: "2022-11-01",
    kontaktinfoTiltaksansvarlige: [],
    sluttdato: "2030-11-30",
  },
];
