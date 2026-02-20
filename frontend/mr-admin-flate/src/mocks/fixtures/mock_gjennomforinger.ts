import { nikolineKontaktperson, petrusKontaktperson } from "@/mocks/fixtures/mock_ansatt";
import { mockAvtaler } from "@/mocks/fixtures/mock_avtaler";
import {
  ArenaMigreringOpphav,
  DataElementStatusVariant,
  GjennomforingDetaljerDto,
  GjennomforingDtoArrangorUnderenhet,
  GjennomforingOppstartstype,
  GjennomforingPameldingType,
  GjennomforingStatusType,
  GjennomforingTiltakstype,
  GjennomforingType,
  PaginatedResponseGjennomforingKompaktDto,
  PrismodellType,
  TiltakstypeDto,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import { mockArrangorKontaktpersoner } from "./mock_arrangorKontaktperson";
import { mockEnheter } from "./mock_enheter";
import { mockTiltakstyper } from "./mock_tiltakstyper";

const arrangor: GjennomforingDtoArrangorUnderenhet = {
  id: "d9d4db51-3564-4493-b897-4fc38dc48965",
  organisasjonsnummer: "992943084",
  navn: "FRETEX AS AVD OSLO",
  kontaktpersoner: mockArrangorKontaktpersoner,
  slettet: false,
};

export const mockGjennomforinger: GjennomforingDetaljerDto[] = [
  {
    tiltakstype: getGjennomforingTiltakstype(mockTiltakstyper.AVKLARAG),
    gjennomforing: {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee361",
      navn: "Yrkesnorsk med praksis med en veldig lang tittel som ikke er helt utenkelig at de skriver inn",
      tiltaksnummer: "2023#123456",
      lopenummer: "2023#123456",
      antallPlasser: 50,
      arrangor,
      avtaleId: mockAvtaler[0].id,
      administratorer: [{ navIdent: "B123456", navn: "Bertil Bengtson" }],
      startDato: "2022-01-01",
      sluttDato: "2029-12-12",
      deltidsprosent: 100,
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
      tilgjengeligForArrangorDato: null,
      pameldingType: GjennomforingPameldingType.DIREKTE_VEDTAK,
      stengt: [],
      apentForPamelding: false,
    },
    veilederinfo: {
      estimertVentetid: { verdi: 3, enhet: "maned" },
      kontorstruktur: [
        {
          region: mockEnheter._0300,
          kontorer: [mockEnheter._0313, mockEnheter._0318, mockEnheter._0315, mockEnheter._0330],
        },
      ],
      oppmoteSted: null,
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
    },
    amoKategorisering: null,
    utdanningslop: null,
    prismodell: {
      id: "d136d6a4-c812-4d28-81db-b688187e4e32",
      type: PrismodellType.ANNEN_AVTALT_PRIS,
      valuta: Valuta.NOK,
      navn: "Annen avtalt pris",
      beskrivelse: [],
      satser: null,
      prisbetingelser: "Maskert prisbetingelser",
    },
  },
  {
    tiltakstype: getGjennomforingTiltakstype(mockTiltakstyper.ARBFORB),
    gjennomforing: {
      pameldingType: GjennomforingPameldingType.DIREKTE_VEDTAK,
      id: "a7d63fb0-4366-412c-84b7-7c15518ee362",
      navn: "Spillbasert kvalifisering",
      tiltaksnummer: "2024#123456",
      lopenummer: "2024#123456",
      deltidsprosent: 100,
      arrangor,
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      administratorer: [],
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
      tilgjengeligForArrangorDato: null,
      stengt: [],
      antallPlasser: 0,
      avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
      apentForPamelding: true,
    },
    veilederinfo: {
      kontorstruktur: [],
      kontaktpersoner: [],
      publisert: false,
      oppmoteSted: null,
      faneinnhold: null,
      beskrivelse: null,
      estimertVentetid: null,
    },
    amoKategorisering: null,
    utdanningslop: null,
    prismodell: {
      id: "d136d6a4-c812-4d28-81db-b688187e4e32",
      type: PrismodellType.ANNEN_AVTALT_PRIS,
      valuta: Valuta.NOK,
      navn: "Annen avtalt pris",
      beskrivelse: [],
      satser: null,
      prisbetingelser: "Maskert prisbetingelser",
    },
  },
  {
    tiltakstype: getGjennomforingTiltakstype(mockTiltakstyper.GRUFAGYRKE),
    gjennomforing: {
      id: "a7d63fb0-4366-412c-84b7-7c15518ee364",
      navn: "Tiltak hos Kulinarisk akademi",
      tiltaksnummer: "2025#123456",
      lopenummer: "2025#123456",
      deltidsprosent: 100,
      arrangor,
      avtaleId: mockAvtaler[3].id,
      startDato: "2022-01-01",
      sluttDato: "2022-12-12",
      administratorer: [],
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
      tilgjengeligForArrangorDato: null,
      stengt: [],
      antallPlasser: 0,
      pameldingType: GjennomforingPameldingType.DIREKTE_VEDTAK,
      apentForPamelding: true,
    },
    veilederinfo: {
      kontorstruktur: [],
      kontaktpersoner: [],
      publisert: false,
      oppmoteSted: null,
      faneinnhold: null,
      beskrivelse: null,
      estimertVentetid: null,
    },
    amoKategorisering: null,
    utdanningslop: null,
    prismodell: {
      id: "d136d6a4-c812-4d28-81db-b688187e4e32",
      type: PrismodellType.ANNEN_AVTALT_PRIS,
      valuta: Valuta.NOK,
      navn: "Annen avtalt pris",
      beskrivelse: [],
      satser: null,
      prisbetingelser: "Maskert prisbetingelser",
    },
  },
];

export const paginertMockGjennomforinger: PaginatedResponseGjennomforingKompaktDto = {
  pagination: {
    totalCount: 18,
    pageSize: 50,
    totalPages: 1,
  },
  data: mockGjennomforinger.map((detaljer) => {
    return {
      tiltakstype: detaljer.tiltakstype,
      publisert: detaljer.veilederinfo?.publisert || false,
      kontorstruktur: detaljer.veilederinfo?.kontorstruktur || [],
      ...detaljer.gjennomforing,
      type: GjennomforingType.AVTALE,
    };
  }),
};

function getGjennomforingTiltakstype(dto: TiltakstypeDto): GjennomforingTiltakstype {
  return {
    id: dto.id,
    navn: dto.navn,
    tiltakskode: dto.tiltakskode,
  };
}
