import { nikolineKontaktperson, petrusKontaktperson } from "@/mocks/fixtures/mock_ansatt";
import { mockAvtaler } from "@/mocks/fixtures/mock_avtaler";
import {
  AmoKategoriseringBransjeOgYrkesrettetBransje as Bransje,
  AmoKategoriseringBransjeOgYrkesrettetForerkortKlasse as ForerkortKlasse,
  AmoKategoriseringInnholdElement as InnholdElement,
  ArenaMigreringOpphav,
  DataElementStatusVariant,
  GjennomforingArrangorUnderenhet,
  GjennomforingDto,
  GjennomforingOppstartstype,
  GjennomforingStatusType,
  GjennomforingTiltakstype,
  LabeledDataElementType,
  PaginatedResponseGjennomforingDto,
  TiltakstypeDto,
} from "@tiltaksadministrasjon/api-client";
import { mockArrangorKontaktpersoner } from "./mock_arrangorKontaktperson";
import { mockEnheter } from "./mock_enheter";
import { mockTiltakstyper } from "./mock_tiltakstyper";

const arrangor: GjennomforingArrangorUnderenhet = {
  id: "d9d4db51-3564-4493-b897-4fc38dc48965",
  organisasjonsnummer: "992943084",
  navn: "FRETEX AS AVD OSLO",
  kontaktpersoner: mockArrangorKontaktpersoner,
  slettet: false,
};

export const mockGjennomforinger: GjennomforingDto[] = [
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee361",
    navn: "Yrkesnorsk med praksis med en veldig lang tittel som ikke er helt utenkelig at de skriver inn",
    tiltaksnummer: "123456",
    estimertVentetid: {
      type: LabeledDataElementType.INLINE,
      label: "Estimert ventetid",
      value: {
        type: "no.nav.mulighetsrommet.model.DataElement.Text",
        value: "3 måneder",
        format: null,
      },
    },
    antallPlasser: 50,
    arrangor,
    avtaleId: mockAvtaler[0].id,
    tiltakstype: getGjennomforingTiltakstype(mockTiltakstyper.AVKLARAG),
    administratorer: [
      {
        navIdent: "B123456",
        navn: "Bertil Bengtson",
      },
    ],
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
    status: {
      type: GjennomforingStatusType.GJENNOMFORES,
      status: {
        value: "Gjennomføres",
        variant: DataElementStatusVariant.SUCCESS,
        description: null,
      },
    },
    oppstart: GjennomforingOppstartstype.LOPENDE,
    opphav: ArenaMigreringOpphav.TILTAKSADMINISTRASJON,
    apentForPamelding: false,
    stedForGjennomforing: "Brummundal",
    kontaktpersoner: [petrusKontaktperson, nikolineKontaktperson],
    publisert: true,
    beskrivelse: "bla bla bla beskrivelse",
    faneinnhold: {
      forHvemInfoboks: null,
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
      detaljerOgInnhold: null,
      kontaktinfo: null,
      kontaktinfoInfoboks: null,
      oppskrift: null,
      delMedBruker: null,
    },
    tilgjengeligForArrangorDato: null,
    amoKategorisering: null,
    stengt: [],
    utdanningslop: null,
  },
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee362",
    navn: "Spillbasert kvalifisering",
    tiltaksnummer: "123456",
    deltidsprosent: 100,
    arrangor,
    tiltakstype: getGjennomforingTiltakstype(mockTiltakstyper.ARBFORB),
    startDato: "2022-01-01",
    sluttDato: "2022-12-12",
    arenaAnsvarligEnhet: mockEnheter._0313,
    administratorer: [],
    kontorstruktur: [],
    status: {
      type: GjennomforingStatusType.AVLYST,
      status: {
        value: "Avlyst",
        variant: DataElementStatusVariant.SUCCESS,
        description:
          "Denne gjennomføringen ble avbrutt pga av noe som ikke var listen opp i listen over mulige årsaker.",
      },
    },
    oppstart: GjennomforingOppstartstype.LOPENDE,
    opphav: ArenaMigreringOpphav.TILTAKSADMINISTRASJON,
    apentForPamelding: true,
    kontaktpersoner: [],
    publisert: false,
    tilgjengeligForArrangorDato: null,
    amoKategorisering: null,
    stengt: [],
    antallPlasser: 0,
    avtaleId: null,
    stedForGjennomforing: null,
    faneinnhold: null,
    beskrivelse: null,
    estimertVentetid: null,
    utdanningslop: null,
  },
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee363",
    navn: "Gruppe AMO",
    tiltaksnummer: "654434",
    deltidsprosent: 100,
    arrangor,
    tiltakstype: getGjennomforingTiltakstype(mockTiltakstyper.GRUPPEAMO),
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
    status: {
      type: GjennomforingStatusType.GJENNOMFORES,
      status: {
        value: "Gjennomføres",
        variant: DataElementStatusVariant.SUCCESS,
        description: null,
      },
    },
    oppstart: GjennomforingOppstartstype.FELLES,
    opphav: ArenaMigreringOpphav.TILTAKSADMINISTRASJON,
    apentForPamelding: true,
    antallPlasser: 10,
    kontaktpersoner: [],
    publisert: false,
    tilgjengeligForArrangorDato: null,
    amoKategorisering: {
      kurstype: "BRANSJE_OG_YRKESRETTET",
      bransje: Bransje.ANDRE_BRANSJER,
      forerkort: [ForerkortKlasse.A, ForerkortKlasse.S],
      innholdElementer: [InnholdElement.GRUNNLEGGENDE_FERDIGHETER],
      sertifiseringer: [{ konseptId: 12, label: "Truckførerkurs" }],
    },
    stengt: [],
    avtaleId: null,
    stedForGjennomforing: null,
    faneinnhold: null,
    beskrivelse: null,
    estimertVentetid: null,
    utdanningslop: null,
  },
  {
    id: "a7d63fb0-4366-412c-84b7-7c15518ee364",
    navn: "Tiltak hos Kulinarisk akademi",
    tiltaksnummer: "654432",
    deltidsprosent: 100,
    arrangor,
    tiltakstype: getGjennomforingTiltakstype(mockTiltakstyper.GRUFAGYRKE),
    avtaleId: mockAvtaler[3].id,
    startDato: "2022-01-01",
    sluttDato: "2022-12-12",
    arenaAnsvarligEnhet: mockEnheter._0313,
    administratorer: [],
    kontorstruktur: [],
    status: {
      type: GjennomforingStatusType.GJENNOMFORES,
      status: {
        value: "Gjennomføres",
        variant: DataElementStatusVariant.SUCCESS,
        description: null,
      },
    },
    oppstart: GjennomforingOppstartstype.LOPENDE,
    opphav: ArenaMigreringOpphav.TILTAKSADMINISTRASJON,
    apentForPamelding: true,
    kontaktpersoner: [],
    publisert: false,

    tilgjengeligForArrangorDato: null,
    amoKategorisering: null,
    stengt: [],
    antallPlasser: 0,
    stedForGjennomforing: null,
    faneinnhold: null,
    beskrivelse: null,
    estimertVentetid: null,
    utdanningslop: null,
  },
];

export const paginertMockGjennomforinger: PaginatedResponseGjennomforingDto = {
  pagination: {
    totalCount: 18,
    pageSize: 50,
    totalPages: 1,
  },
  data: mockGjennomforinger,
};

function getGjennomforingTiltakstype(dto: TiltakstypeDto): GjennomforingTiltakstype {
  if (!dto.tiltakskode) {
    throw new Error("Tiltakskode mangler");
  }

  return {
    id: dto.id,
    navn: dto.navn,
    tiltakskode: dto.tiltakskode,
  };
}
