import {
  ArrangorflateTilsagnDto,
  DataDetails,
  DataDrivenTableDto,
  DataDrivenTableDtoColumnAlign,
  DataDrivenTableDtoRow,
  DataElementStatusVariant,
  DataElementTextFormat,
  LabeledDataElementType,
  TilsagnStatus,
  TilsagnType,
  Tiltakskode,
  Valuta,
} from "api-client";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";
import {
  dataElementLink,
  dataElementPeriode,
  dataElementStatus,
  dataElementText,
} from "./dataDrivenTableHelpers";

const beregningManedspris: DataDetails = {
  header: null,
  entries: [
    {
      label: "Tilsagnsperiode",
      type: LabeledDataElementType.INLINE,
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "01.10.2025 - 06.11.2025",
        format: null,
      },
    },
    {
      label: "Antall plasser",
      type: LabeledDataElementType.INLINE,
      value: {
        value: "100",
        type: "DATA_ELEMENT_TEXT",
        format: DataElementTextFormat.NUMBER,
      },
    },
    {
      label: "Avtalt månedspris per tiltaksplass",
      type: LabeledDataElementType.INLINE,
      value: {
        value: "10000",
        type: "DATA_ELEMENT_MONEY_AMOUNT",
        currency: "NOK",
      },
    },
    {
      label: "Totalbeløp",
      type: LabeledDataElementType.INLINE,
      value: {
        value: "1200000",
        type: "DATA_ELEMENT_MONEY_AMOUNT",
        currency: "NOK",
      },
    },
    {
      label: "Gjenstående beløp",
      type: LabeledDataElementType.INLINE,
      value: {
        value: "1200000",
        type: "DATA_ELEMENT_MONEY_AMOUNT",
        currency: "NOK",
      },
    },
  ],
};

const beregningUkespris: DataDetails = {
  header: null,
  entries: [
    {
      type: LabeledDataElementType.INLINE,
      label: "Tilsagnsperiode",
      value: {
        type: "DATA_ELEMENT_PERIODE",
        start: "01.08.2025",
        slutt: "31.12.2025",
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Antall plasser",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "333",
        format: DataElementTextFormat.NUMBER,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Avtalt ukespris per tiltaksplass",
      value: {
        type: "DATA_ELEMENT_MONEY_AMOUNT",
        value: "5015",
        currency: "NOK",
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Totalbeløp",
      value: {
        type: "DATA_ELEMENT_MONEY_AMOUNT",
        value: "36405891",
        currency: "NOK",
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Gjenstående beløp",
      value: {
        type: "DATA_ELEMENT_MONEY_AMOUNT",
        value: "35105891",
        currency: "NOK",
      },
    },
  ],
};

const beregningFastSats: DataDetails = {
  header: null,
  entries: [
    {
      type: LabeledDataElementType.INLINE,
      label: "Tilsagnsperiode",
      value: {
        type: "DATA_ELEMENT_PERIODE",
        start: "01.07.2025",
        slutt: "31.12.2025",
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Antall plasser",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "10",
        format: DataElementTextFormat.NUMBER,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Sats per tiltaksplass per måned",
      value: {
        type: "DATA_ELEMENT_MONEY_AMOUNT",
        value: "20975",
        currency: "NOK",
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Totalbeløp",
      value: {
        type: "DATA_ELEMENT_MONEY_AMOUNT",
        value: "1258500",
        currency: "NOK",
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Gjenstående beløp",
      value: {
        type: "DATA_ELEMENT_MONEY_AMOUNT",
        value: "1139293",
        currency: "NOK",
      },
    },
  ],
};

const avklaringManedsprisTilsagn: ArrangorflateTilsagnDto = {
  id: "5b08cd43-102e-4845-889e-99c5de2bc252",
  tiltakstype: {
    navn: "Avklaring",
    tiltakskode: Tiltakskode.AVKLARING,
  },
  gjennomforing: {
    id: "70cdc182-8913-48c0-bad9-fa4e74f3288e",
    navn: "Avklaring - avtalt månedspris",
    lopenummer: "2025/10000",
  },
  arrangor: arrangorMock,
  type: TilsagnType.TILSAGN,
  periode: { start: "2025-10-01", slutt: "2025-11-07" },
  status: TilsagnStatus.GODKJENT,
  bruktBelop: { belop: 0, valuta: Valuta.NOK },
  gjenstaendeBelop: { belop: 1200000, valuta: Valuta.NOK },
  beregning: beregningManedspris,
  bestillingsnummer: "A-2025/12611-1",
  beskrivelse: null,
};

const avklaringManedsprisTilsagnRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Avklaring (2025/10000)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({
      start: "2025-10-01",
      slutt: "2025-11-07",
    }),
    tilsagn: dataElementText("Tilsagn (A-2025/12611-1)"),
    status: dataElementStatus("Godkjent", DataElementStatusVariant.SUCCESS),
    action: dataElementLink(
      "Se detaljer",
      `${arrangorMock.organisasjonsnummer}/tilsagn/${avklaringManedsprisTilsagn.id}`,
    ),
  },
};

const arrUkesprisTilsagn: ArrangorflateTilsagnDto = {
  id: "a7e0df87-f37e-4f6a-92d6-a25a1cded9e7",
  tiltakstype: {
    navn: "Arbeidsrettet rehabilitering",
    tiltakskode: Tiltakskode.ARBEIDSRETTET_REHABILITERING,
  },
  gjennomforing: {
    id: "a47092ba-410b-4ca1-9713-36506a039742",
    navn: "Arbeidsrettet rehabilitering - avtalt ukespris",
    lopenummer: "2025/10001",
  },
  arrangor: arrangorMock,
  type: TilsagnType.TILSAGN,
  periode: { start: "2025-10-01", slutt: "2025-11-01" },
  status: TilsagnStatus.GODKJENT,
  bruktBelop: { belop: 0, valuta: Valuta.NOK },
  gjenstaendeBelop: { belop: 248400, valuta: Valuta.NOK },
  beregning: beregningUkespris,
  bestillingsnummer: "A-2025/4123-1",
  beskrivelse: null,
};
const arrUkesprisTilsagnRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Arbeidsrettet rehabilitering(2025/10001)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({ start: "2025-10-01", slutt: "2025-11-01" }),
    tilsagn: dataElementText("Tilsagn (A-2025/4123-1)"),
    status: dataElementStatus("Godkjent", DataElementStatusVariant.SUCCESS),
    action: dataElementLink(
      "Se detaljer",
      `${arrangorMock.organisasjonsnummer}/tilsagn/${arrUkesprisTilsagn.id}`,
    ),
  },
};
const aftFoobarTilsagnGodkjent: ArrangorflateTilsagnDto = {
  id: "ad77762c-eebb-4623-be6d-0c64da79f2dd",
  gjennomforing: {
    id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
    navn: "AFT Foobar",
    lopenummer: "2025/10001",
  },
  bruktBelop: { belop: 51205, valuta: Valuta.NOK },
  gjenstaendeBelop: { belop: 5234495, valuta: Valuta.NOK },
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  type: TilsagnType.TILSAGN,
  periode: {
    start: "2025-01-01",
    slutt: "2025-07-01",
  },
  beregning: beregningFastSats,
  arrangor: arrangorMock,
  status: TilsagnStatus.GODKJENT,
  bestillingsnummer: "A-2025/11073-1",
  beskrivelse: null,
};

const aftFoobarTilsagnGodkjentRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Arbeidsforberedende trening (2025/10001)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({
      start: "2025-01-01",
      slutt: "2025-07-01",
    }),
    tilsagn: dataElementText("Tilsagn (A-2025/11073-1)"),
    status: dataElementStatus("Godkjent", DataElementStatusVariant.SUCCESS),
    action: dataElementLink(
      "Se detaljer",
      `${arrangorMock.organisasjonsnummer}/tilsagn/${aftFoobarTilsagnGodkjent.id}`,
    ),
  },
};

const aftFoobarInvesteringTilsagnGodkjent: ArrangorflateTilsagnDto = {
  id: "d8ccb57f-b9db-48e1-97f1-cb38426a9389",
  gjennomforing: {
    id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
    navn: "AFT Foobar",
    lopenummer: "2025/10001",
  },
  bruktBelop: { belop: 0, valuta: Valuta.NOK },
  gjenstaendeBelop: { belop: 123456, valuta: Valuta.NOK },
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  type: TilsagnType.INVESTERING,
  periode: {
    start: "2025-01-01",
    slutt: "2025-12-31",
  },
  beregning: beregningFastSats,
  arrangor: arrangorMock,
  status: TilsagnStatus.GODKJENT,
  bestillingsnummer: "A-2025/11073-2",
  beskrivelse: null,
};

const aftFoobarInvesteringTilsagnGodkjentRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Arbeidsforberedende trening (2025/10001)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({
      start: "2025-01-01",
      slutt: "2025-12-31",
    }),
    tilsagn: dataElementText("Tilsagn for investeringer (A-2025/11073-2)"),
    status: dataElementStatus("Godkjent", DataElementStatusVariant.SUCCESS),
    action: dataElementLink(
      "Se detaljer",
      `${arrangorMock.organisasjonsnummer}/tilsagn/${aftFoobarInvesteringTilsagnGodkjent.id}`,
    ),
  },
};

const avklaringTilsagnAnnulert: ArrangorflateTilsagnDto = {
  id: "f8fbc0f7-3280-410b-8387-20ff63896926",
  gjennomforing: {
    id: "a47092ba-410b-4ca1-9713-c6506a039742",
    navn: "Avklaringen sin det",
    lopenummer: "2025/10002",
  },
  bruktBelop: { belop: 0, valuta: Valuta.NOK },
  gjenstaendeBelop: { belop: 0, valuta: Valuta.NOK },
  tiltakstype: {
    navn: "Avklaring",
    tiltakskode: Tiltakskode.AVKLARING,
  },
  type: TilsagnType.TILSAGN,
  periode: {
    start: "2025-04-01",
    slutt: "2025-05-01",
  },
  beregning: beregningManedspris,
  arrangor: arrangorMock,
  status: TilsagnStatus.ANNULLERT,
  bestillingsnummer: "A-2025/11147-2",
  beskrivelse: null,
};

const avklaringTilsagnAnnulertRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Avklaring (2025/10002)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({
      start: "2025-04-01",
      slutt: "2025-05-01",
    }),
    tilsagn: dataElementText("Tilsagn (A-2025/11147-2)"),
    status: dataElementStatus("Annullert", DataElementStatusVariant.ERROR_BORDER_STRIKETHROUGH),
    action: dataElementLink(
      "Se detaljer",
      `${arrangorMock.organisasjonsnummer}/tilsagn/${avklaringTilsagnAnnulert.id}`,
    ),
  },
};

const aftFoobarEkstraTilsagnGodkjent: ArrangorflateTilsagnDto = {
  id: "17a7ff74-648b-4d43-a27d-d26cc2553b3b",
  gjennomforing: {
    id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
    navn: "AFT Foobar",
    lopenummer: "2025/10001",
  },
  bruktBelop: { belop: 55, valuta: Valuta.NOK },
  gjenstaendeBelop: { belop: 0, valuta: Valuta.NOK },
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  type: TilsagnType.EKSTRATILSAGN,
  periode: {
    start: "2025-03-18",
    slutt: "2025-04-02",
  },
  beregning: beregningFastSats,
  arrangor: arrangorMock,
  status: TilsagnStatus.OPPGJORT,
  bestillingsnummer: "A-2025/11073-3",
  beskrivelse: null,
};

const aftFoobarEkstraTilsagnGodkjentRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Arbeidsforberedende trening (2025/10001)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({
      start: "2025-03-18",
      slutt: "2025-04-02",
    }),
    tilsagn: dataElementText("Ekstra tilsagn (A-2025/11073-3)"),
    status: dataElementStatus("Oppgjort", DataElementStatusVariant.NEUTRAL),
    action: dataElementLink(
      "Se detaljer",
      `${arrangorMock.organisasjonsnummer}/tilsagn/${aftFoobarEkstraTilsagnGodkjent.id}`,
    ),
  },
};

const mayRainVTATilsagnGodkjent: ArrangorflateTilsagnDto = {
  id: "27f81471-1c6a-4f68-921e-ba9da68d4e89",
  gjennomforing: {
    id: "6a760ab8-fb12-4c6e-b143-b711331f63f6",
    navn: "May rain - VTA",
    lopenummer: "2025/10003",
  },
  bruktBelop: { belop: 0, valuta: Valuta.NOK },
  gjenstaendeBelop: { belop: 6000, valuta: Valuta.NOK },

  tiltakstype: {
    navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
    tiltakskode: Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
  },
  type: TilsagnType.TILSAGN,
  periode: { start: "2025-04-01", slutt: "2025-10-01" },
  beregning: beregningFastSats,
  arrangor: arrangorMock,
  status: TilsagnStatus.GODKJENT,
  bestillingsnummer: "A-2025/11398-1",
  beskrivelse: null,
};

const mayRainVTATilsagnGodkjentRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Varig tilrettelagt arbeid i skjermet virksomhet (2025/10003)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({ start: "2025-04-01", slutt: "2025-10-01" }),
    tilsagn: dataElementText("Tilsagn (A-2025/11398-1)"),
    status: dataElementStatus("Godkjent", DataElementStatusVariant.SUCCESS),
    action: dataElementLink(
      "Se detaljer",
      `${arrangorMock.organisasjonsnummer}/tilsagn/${mayRainVTATilsagnGodkjent.id}`,
    ),
  },
};

export const arrangorflateTilsagn: ArrangorflateTilsagnDto[] = [
  aftFoobarTilsagnGodkjent,
  aftFoobarInvesteringTilsagnGodkjent,
  avklaringTilsagnAnnulert,
  aftFoobarEkstraTilsagnGodkjent,
  mayRainVTATilsagnGodkjent,
  avklaringManedsprisTilsagn,
  arrUkesprisTilsagn,
];

export const tilsagnOversikt: DataDrivenTableDto = {
  columns: [
    { key: "tiltak", label: "Tiltak", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
    {
      key: "arrangor",
      label: "Arrangør",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    { key: "periode", label: "Periode", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
    {
      key: "tilsagn",
      label: "Tilsagn",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    { key: "status", label: "Status", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
    {
      key: "action",
      label: "Handlinger",
      sortable: false,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
  ],
  rows: [
    aftFoobarTilsagnGodkjentRow,
    aftFoobarInvesteringTilsagnGodkjentRow,
    avklaringTilsagnAnnulertRow,
    aftFoobarEkstraTilsagnGodkjentRow,
    mayRainVTATilsagnGodkjentRow,
    avklaringManedsprisTilsagnRow,
    arrUkesprisTilsagnRow,
  ],
};
