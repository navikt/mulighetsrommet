import {
  Innsatsgruppe,
  PaginertTiltakstype,
  Tiltakskode,
  TiltakskodeArena,
  TiltakstypeStatus,
  VeilederflateTiltakstype,
} from "@mr/api-client-v2";

export const mockTiltakstyper = {
  ARBFORB: {
    id: "59a64a02-efdd-471d-9529-356ff5553a5d",
    navn: "Arbeidsforberedende trening (AFT)",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    arenaKode: TiltakskodeArena.ARBFORB,
    startDato: "2016-01-01",
    sluttDato: "2099-01-01",
    status: TiltakstypeStatus.AKTIV,
    sanityId: "d03363e0-7d46-411b-aec4-fb9449e30eb8",
  },
  ARBRRHDAG: {
    id: "1d5bf722-02aa-4aa5-97e2-f359ea307a14",
    navn: "Arbeidsrettet rehabilitering (dag)",
    tiltakskode: Tiltakskode.ARBEIDSRETTET_REHABILITERING,
    arenaKode: TiltakskodeArena.ARBRRHDAG,
    startDato: "2012-01-01",
    sluttDato: "2099-01-01",
    status: TiltakstypeStatus.AKTIV,
    sanityId: "29c3d3cb-ffbf-4c22-8ffc-fea5d7f6c822",
  },
  AVKLARAG: {
    id: "938c2e7b-91d6-4eee-97d3-a110ccbc5968",
    navn: "Avklaring",
    tiltakskode: Tiltakskode.AVKLARING,
    arenaKode: TiltakskodeArena.AVKLARAG,
    startDato: "2009-01-01",
    sluttDato: "2099-01-01",
    status: TiltakstypeStatus.AKTIV,
    sanityId: "f9618e97-4510-49e2-b748-29cae84d9019",
  },
  DIGIOPPARB: {
    id: "e47447e3-bbe4-4c41-839d-f352130b2e8a",
    navn: "Digitalt jobbsøkerkurs for arbeidsledige (jobbklubb)",
    tiltakskode: Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
    arenaKode: TiltakskodeArena.DIGIOPPARB,
    startDato: "2021-01-01",
    sluttDato: "2099-01-01",
    status: TiltakstypeStatus.AKTIV,
    sanityId: "3526de0d-ad4c-4b81-b072-a13b3a4b4ed3",
  },
  GRUPPEAMO: {
    id: "9b52265c-914c-413d-bca4-e9d7b3f1bd8d",
    navn: "Gruppe AMO",
    tiltakskode: Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    arenaKode: TiltakskodeArena.GRUPPEAMO,
    startDato: "2019-07-01",
    sluttDato: "2099-01-01",
    status: TiltakstypeStatus.AKTIV,
    sanityId: "eadeb22c-bd89-4298-a5c2-145f112f8e7d",
  },
  GRUFAGYRKE: {
    id: "53ecc473-c0ce-40ea-88c3-9f4a3131080b",
    navn: "Gruppe Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning",
    tiltakskode: Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
    arenaKode: TiltakskodeArena.GRUFAGYRKE,
    startDato: "2019-07-01",
    sluttDato: "2099-01-01",
    status: TiltakstypeStatus.AKTIV,
    sanityId: "7f353dcd-37c2-42f8-bab6-ac2a60669839",
  },
  JOBBK: {
    id: "95766f55-a456-4c4b-9a77-ca61ae214409",
    navn: "Jobbklubb",
    tiltakskode: Tiltakskode.JOBBKLUBB,
    arenaKode: TiltakskodeArena.JOBBK,
    startDato: "2003-10-10",
    sluttDato: "2099-01-01",
    status: TiltakstypeStatus.AKTIV,
    sanityId: "31e72dd8-ad05-4e81-a7f9-fd4c8f295864",
  },
  INDOPPFAG: {
    id: "71a51692-35c5-4951-84eb-a338b0a57210",
    navn: "Oppfølging",
    tiltakskode: Tiltakskode.OPPFOLGING,
    arenaKode: TiltakskodeArena.INDOPPFAG,
    startDato: "2009-01-01",
    sluttDato: "2099-01-01",
    status: TiltakstypeStatus.AKTIV,
    sanityId: "5ac48c03-1f4c-4d4b-b862-050caca92080",
  },
  VASV: {
    id: "6fb921d6-0a87-4b8a-82a4-067477c1e113",
    navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
    tiltakskode: Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    arenaKode: TiltakskodeArena.VASV,
    startDato: "2001-01-01",
    sluttDato: "2099-01-01",
    status: TiltakstypeStatus.AKTIV,
    sanityId: "02509279-0a0f-4bd6-b506-f40111e4ba14",
  },
};

export const paginertMockTiltakstyper: PaginertTiltakstype = {
  pagination: {
    totalCount: Object.values(mockTiltakstyper).length,
    pageSize: 50,
    totalPages: 1,
  },
  data: Object.values(mockTiltakstyper),
};

export const mockVeilederflateTiltakstypeAFT: VeilederflateTiltakstype = {
  beskrivelse:
    "AFT er et tiltak som skal bidra til å prøve ut tiltaksdeltakerens arbeidsevne og til å styrke mulighetene for å få ordinært arbeid. Det er et tiltak med individuell tilrettelegging som kan tilbys personer med sammensatte bistandsbehov som har fått sin arbeidsevne nedsatt og som har særlig usikre yrkesmessige forutsetninger.",
  regelverkLenker: [
    {
      _id: "123",
      regelverkLenkeNavn: "Regelverk",
      regelverkUrl: "https://www.google.no",
    },
    {
      _id: "1234",
      regelverkLenkeNavn: "Rundskriv",
      regelverkUrl: "https://www.google.no",
    },
  ],
  sanityId: "d03363e0-7d46-411b-aec4-fb9449e30eb8",
  navn: "AFT - Arbeidsforberedende trening",
  faneinnhold: {
    forHvemInfoboks: "For hvem infoboks med alert informasjon",
    forHvem: [
      {
        _type: "block",
        style: "normal",
        _key: "11838589f53b",
        markDefs: [],
        children: [
          {
            _key: "ae9285699b2d",
            _type: "span",
            marks: [],
            text: "AFT er et individuelt tiltak som kan tilbys personer med sammensatte bistandsbehov som har fått sin arbeidsevne nedsatt og som har særlig usikre yrkesmessige forutsetninger.",
          },
        ],
      },
    ],
    detaljerOgInnhold: [
      {
        children: [
          {
            _type: "span",
            marks: [],
            text: "Det skal være individuelt tilpasset og tilrettelagt brukerens behov, og kan inneholde:",
            _key: "efae61d95684",
          },
        ],
        _type: "block",
        style: "normal",
        _key: "0563b7b58f3e",
        markDefs: [],
      },
      {
        level: 1,
        _type: "block",
        style: "normal",
        _key: "0afce2c1e8fc",
        listItem: "bullet",
        markDefs: [],
        children: [
          {
            _type: "span",
            marks: [],
            text: "Avklaring og kartlegging av ressurser",
            _key: "2aaf9c415a20",
          },
        ],
      },
      {
        style: "normal",
        _key: "9d9ec76d1ef9",
        listItem: "bullet",
        markDefs: [],
        children: [
          {
            marks: [],
            text: "Karriereveiledning",
            _key: "2f578003a710",
            _type: "span",
          },
        ],
        level: 1,
        _type: "block",
      },
      {
        markDefs: [],
        children: [
          {
            _key: "f07a9b815d8d",
            _type: "span",
            marks: [],
            text: "Utprøving av arbeidsevne i et tilrettelagt arbeidsmiljø",
          },
        ],
        level: 1,
        _type: "block",
        style: "normal",
        _key: "a44028e88775",
        listItem: "bullet",
      },
      {
        _key: "990c7a9b466f",
        listItem: "bullet",
        markDefs: [],
        children: [
          {
            marks: [],
            text: "Opplæring med sikte på å oppnå reelle ferdigheter og formell kompetanse",
            _key: "b4171fad86f6",
            _type: "span",
          },
        ],
        level: 1,
        _type: "block",
        style: "normal",
      },
      {
        level: 1,
        _type: "block",
        style: "normal",
        _key: "c274808be39f",
        listItem: "bullet",
        markDefs: [],
        children: [
          {
            text: "Arbeidstrening og oppfølging i ordinært arbeidsliv",
            _key: "56d70aad3d65",
            _type: "span",
            marks: [],
          },
        ],
      },
      {
        _key: "c274808be39f_deduped_6",
        listItem: "bullet",
        markDefs: [],
        children: [
          {
            _key: "56d70aad3d65",
            _type: "span",
            marks: [],
            text: "Arbeidstrening og oppfølging i ordinært arbeidsliv",
          },
        ],
        level: 1,
        _type: "block",
        style: "normal",
      },
    ],
    pameldingOgVarighet: [
      {
        _type: "block",
        style: "normal",
        _key: "db4fe84d3fd3",
        markDefs: [],
        children: [
          {
            _type: "span",
            marks: [],
            text: "Varigheten skal tilpasses deltagerens individuelle behov, og kan vare i inntil ett år, med mulighet for forlengelse i ytterligere ett år.",
            _key: "5e130c0d19be",
          },
        ],
      },
      {
        style: "normal",
        _key: "b7241288003d",
        markDefs: [],
        children: [
          {
            marks: [],
            text: "",
            _key: "c3168f988e7c",
            _type: "span",
          },
        ],
        _type: "block",
      },
      {
        markDefs: [],
        children: [
          {
            _type: "span",
            marks: [],
            text: "Det finnes ikke et eget innsøkingsskjema til AFT. Innsøket gjøres i Arena og det genereres en henvisning som skal oversendes arrangør.",
            _key: "4b0e52391dc5",
          },
        ],
        _type: "block",
        style: "normal",
        _key: "f531b753b7d7",
      },
    ],
  },
  innsatsgrupper: [
    Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
    Innsatsgruppe.JOBBE_DELVIS,
    Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
  ],
  delingMedBruker:
    "Hei <Fornavn>, \n\nVi har nå et arbeidsmarkedstiltak som jeg tenker kan passe deg godt. Det heter <tiltaksnavn> og er et tilbud for deg som deg som har nedsatt arbeidsevne og trenger hjelp for å komme i jobb.\n\nMålet er å avklare arbeidsevnen din gjennom arbeidstrening i ulike arbeidssituasjoner.\n\n- Etter en periode med forberedende arbeidstrening i et tilrettelagt arbeidsmiljø får du arbeidstrening i en vanlig bedrift.\n- Du får kartlagt kompetansen din og får karriereveiledning.\n- Du kan få tilrettelagt opplæring hvis du ønsker å gå videre med et yrkesfaglig utdanningsløp. Opplæringen skal bedre mulighetene dine til å komme i jobb.\n\n[Du kan lese mer om kurset på nav.no](www.nav.no/arbeidsforberedende-trening)",
  kanKombineresMed: [],
};
