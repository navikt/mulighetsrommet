import { nikolineKontaktperson, petrusKontaktperson } from "@/mocks/fixtures/mock_ansatt";
import { mockAvtaler } from "@/mocks/fixtures/mock_avtaler";
import {
  Bransje,
  EstimertVentetidEnhet,
  ForerkortKlasse,
  GjennomforingDto,
  GjennomforingOppstartstype,
  GjennomforingStatus,
  InnholdElement,
  Kurstype,
  Opphav,
  PaginertGjennomforing,
} from "@mr/api-client-v2";
import { mockArrangorKontaktpersoner } from "./mock_arrangorKontaktperson";
import { mockArrangorer } from "./mock_arrangorer";
import { mockEnheter } from "./mock_enheter";
import { mockTiltakstyper } from "./mock_tiltakstyper";

export const mockGjennomforinger: GjennomforingDto[] = [
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee361",
    navn: "Yrkesnorsk med praksis med en veldig lang tittel som ikke er helt utenkelig at de skriver inn",
    tiltaksnummer: "123456",
    estimertVentetid: {
      verdi: 3,
      enhet: EstimertVentetidEnhet.MANED,
    },
    antallPlasser: 50,
    arrangor: {
      ...mockArrangorer.data[0].underenheter![0],
      slettet: false,
      kontaktpersoner: [mockArrangorKontaktpersoner[0], mockArrangorKontaktpersoner[0]],
    },
    avtaleId: mockAvtaler[0].id,
    tiltakstype: mockTiltakstyper.AVKLARAG,
    administratorer: [
      {
        navIdent: "B123456",
        navn: "Bertil Bengtson",
      },
    ],
    sanityId: "123",
    startDato: "2022-01-01",
    sluttDato: "2029-12-12",
    deltidsprosent: 100,
    arenaAnsvarligEnhet: mockEnheter._0313,
    kontorstruktur: [
      {
        region: mockEnheter._0300,
        kontorer: [mockEnheter._0313, mockEnheter._0318, mockEnheter._0315, mockEnheter._0330],
      },
    ],
    status: { status: GjennomforingStatus.GJENNOMFORES },
    oppstart: GjennomforingOppstartstype.LOPENDE,
    opphav: Opphav.MR_ADMIN_FLATE,
    apentForPamelding: false,
    stedForGjennomforing: "Brummundal",
    kontaktpersoner: [petrusKontaktperson, nikolineKontaktperson],
    publisert: true,
    beskrivelse: "bla bla bla beskrivelse",
    faneinnhold: {
      forHvem: [
        {
          _type: "block",
          children: [
            {
              text: 'Richard had a difficult and often reactionary personality, and was defiantly Northern English in outlook. Brix said that he carried "a chip on both shoulders. I remember him talking about fucking southern bastards a lot and not wanting to come to London. He hated London intensely.Skriver noe mer fornuftig her',
              _type: "span",
            },
          ],
          markDefs: [],
        },
      ],
      pameldingOgVarighet: [
        {
          _key: "eb83652d429d",
          _type: "block",
          style: "normal",
          children: [
            {
              _key: "34b0c44b289b0",
              text: "Nulla lacinia lorem erat, ut sollicitudin ex commodo in. Maecenas ac purus at urna accumsan sagittis. Aenean ornare massa lacus, non tempor orci venenatis in.",
              _type: "span",
              marks: [],
            },
          ],
          markDefs: [],
        },
      ],
      detaljerOgInnholdInfoboks:
        "Phasellus a urna eget augue pharetra posuere. Aenean malesuada tortor eros, ut ullamcorper nisi facilisis eget. ",
      pameldingOgVarighetInfoboks:
        "Nullam lobortis massa nibh, luctus molestie lacus venenatis sed.",
      lenker: [
        {
          lenke:
            "https://www.google.comgesgsegrdgrdiogjrdiughrduihgrdhgiurdhgudirhgidruhgudirhgurdhguirdhgiurdhugdhrguirdhuuhuiehfuirdhgiurdhghrduighdriughrduighrdiughdruighduir",
          lenkenavn: "Google.com",
          apneINyFane: false,
          visKunForVeileder: false,
        },
      ],
    },
    tilgjengeligForArrangorFraOgMedDato: null,
    amoKategorisering: null,
    stengt: [],
  },
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee362",
    navn: "Spillbasert kvalifisering",
    tiltaksnummer: "123456",
    deltidsprosent: 100,
    arrangor: {
      ...mockArrangorer.data[0].underenheter![0],
      slettet: false,
      kontaktpersoner: mockArrangorKontaktpersoner,
    },
    tiltakstype: mockTiltakstyper.ARBFORB,
    sanityId: "1234",
    startDato: "2022-01-01",
    sluttDato: "2022-12-12",
    arenaAnsvarligEnhet: mockEnheter._0313,
    administratorer: [],
    kontorstruktur: [],
    status: {
      status: GjennomforingStatus.AVLYST,
      avbrutt: {
        aarsak:
          "Denne gjennomføringen ble avbrutt pga av noe som ikke var listen opp i listen over mulige årsaker.",
        beskrivelse:
          "Denne gjennomføringen ble avbrutt pga av noe som ikke var listen opp i listen over mulige årsaker.",
        tidspunkt: "2020-03-04T12:00:00",
      },
    },
    oppstart: GjennomforingOppstartstype.LOPENDE,
    opphav: Opphav.MR_ADMIN_FLATE,
    apentForPamelding: true,
    kontaktpersoner: [],
    publisert: false,
    tilgjengeligForArrangorFraOgMedDato: null,
    amoKategorisering: null,
    stengt: [],
  },
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee363",
    navn: "Gruppe AMO",
    tiltaksnummer: "654434",
    sanityId: "1234",
    deltidsprosent: 100,
    arrangor: {
      ...mockArrangorer.data[0].underenheter![0],
      slettet: false,
      kontaktpersoner: mockArrangorKontaktpersoner,
    },
    tiltakstype: mockTiltakstyper.GRUPPEAMO,
    administratorer: [
      {
        navIdent: "B815493",
        navn: "Test Testesen",
      },
    ],
    startDato: "2022-01-01",
    sluttDato: "2022-12-12",
    arenaAnsvarligEnhet: mockEnheter._0313,
    kontorstruktur: [],
    status: { status: GjennomforingStatus.GJENNOMFORES },
    oppstart: GjennomforingOppstartstype.FELLES,
    opphav: Opphav.MR_ADMIN_FLATE,
    apentForPamelding: true,
    antallPlasser: 10,
    kontaktpersoner: [],
    publisert: false,
    tilgjengeligForArrangorFraOgMedDato: null,
    amoKategorisering: {
      kurstype: Kurstype.BRANSJE_OG_YRKESRETTET,
      bransje: Bransje.ANDRE_BRANSJER,
      forerkort: [ForerkortKlasse.A, ForerkortKlasse.S],
      innholdElementer: [InnholdElement.GRUNNLEGGENDE_FERDIGHETER],
      sertifiseringer: [{ konseptId: 12, label: "Truckførerkurs" }],
    },
    stengt: [],
  },
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee364",
    navn: "Tiltak hos Kulinarisk akademi",
    tiltaksnummer: "654432",
    sanityId: "1234",
    deltidsprosent: 100,
    arrangor: {
      ...mockArrangorer.data[0].underenheter![0],
      slettet: false,
      kontaktpersoner: mockArrangorKontaktpersoner,
    },
    tiltakstype: mockTiltakstyper.GRUFAGYRKE,
    avtaleId: mockAvtaler[3].id,
    startDato: "2022-01-01",
    sluttDato: "2022-12-12",
    arenaAnsvarligEnhet: mockEnheter._0313,
    administratorer: [],
    kontorstruktur: [],
    status: { status: GjennomforingStatus.GJENNOMFORES },
    oppstart: GjennomforingOppstartstype.LOPENDE,
    opphav: Opphav.MR_ADMIN_FLATE,
    apentForPamelding: true,
    kontaktpersoner: [],
    publisert: false,

    tilgjengeligForArrangorFraOgMedDato: null,
    amoKategorisering: null,
    stengt: [],
  },
];

export const paginertMockGjennomforinger: PaginertGjennomforing = {
  pagination: {
    totalCount: 18,
    pageSize: 50,
    totalPages: 1,
  },
  data: mockGjennomforinger,
};

// Bruker denne for å teste med flere tiltaksgjennomføringer lokalt, men setter den til 0 sånn
// at testene går gjennom.
const x = 0;
for (let i = 0; i < x; i++) {
  mockGjennomforinger.push({
    id: "a7d63fb0-4366-412c-84b7-7c15518ee363",
    navn: "AFT",
    tiltaksnummer: "654434",
    sanityId: "1234",
    deltidsprosent: 100,
    arrangor: {
      ...mockArrangorer.data[0].underenheter![0],
      slettet: false,
      kontaktpersoner: mockArrangorKontaktpersoner,
    },
    tiltakstype: mockTiltakstyper.ARBFORB,
    startDato: "2022-01-01",
    sluttDato: "2022-12-12",
    arenaAnsvarligEnhet: mockEnheter._0313,
    administratorer: [],
    kontorstruktur: [],
    status: { status: GjennomforingStatus.GJENNOMFORES },
    oppstart: GjennomforingOppstartstype.LOPENDE,
    opphav: Opphav.MR_ADMIN_FLATE,
    apentForPamelding: true,
    kontaktpersoner: [],
    publisert: false,

    tilgjengeligForArrangorFraOgMedDato: null,
    amoKategorisering: null,
    stengt: [],
  });
}
