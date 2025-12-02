import {
  ArrangorflateTilsagnDto,
  DataDetails,
  DataElementTextFormat,
  LabeledDataElementType,
  TilsagnStatus,
  TilsagnType,
  Tiltakskode,
} from "api-client";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";

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
        type: "DATA_ELEMENT_TEXT",
        format: DataElementTextFormat.NOK,
      },
    },
    {
      label: "Totalbeløp",
      type: LabeledDataElementType.INLINE,
      value: {
        value: "1200000",
        type: "DATA_ELEMENT_TEXT",
        format: DataElementTextFormat.NOK,
      },
    },
    {
      label: "Gjenstående beløp",
      type: LabeledDataElementType.INLINE,
      value: {
        value: "1200000",
        type: "DATA_ELEMENT_TEXT",
        format: DataElementTextFormat.NOK,
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
        type: "DATA_ELEMENT_TEXT",
        value: "5015",
        format: DataElementTextFormat.NOK,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Totalbeløp",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "36405891",
        format: DataElementTextFormat.NOK,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Gjenstående beløp",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "35105891",
        format: DataElementTextFormat.NOK,
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
        type: "DATA_ELEMENT_TEXT",
        value: "20975",
        format: DataElementTextFormat.NOK,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Totalbeløp",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "1258500",
        format: DataElementTextFormat.NOK,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Gjenstående beløp",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "1139293",
        format: DataElementTextFormat.NOK,
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
  bruktBelop: 0,
  gjenstaendeBelop: 1200000,
  beregning: beregningManedspris,
  bestillingsnummer: "A-2025/12611-1",
  beskrivelse: null,
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
  bruktBelop: 0,
  gjenstaendeBelop: 248400,
  beregning: beregningUkespris,
  bestillingsnummer: "A-2025/4123-1",
  beskrivelse: null,
};

export const arrangorflateTilsagn: ArrangorflateTilsagnDto[] = [
  {
    id: "ad77762c-eebb-4623-be6d-0c64da79f2dd",
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
      lopenummer: "2025/10001",
    },
    bruktBelop: 51205,
    gjenstaendeBelop: 5234495,
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
  },
  {
    id: "d8ccb57f-b9db-48e1-97f1-cb38426a9389",
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
      lopenummer: "2025/10001",
    },
    bruktBelop: 0,
    gjenstaendeBelop: 123456,
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
  },
  {
    id: "f8fbc0f7-3280-410b-8387-20ff63896926",
    gjennomforing: {
      id: "a47092ba-410b-4ca1-9713-c6506a039742",
      navn: "Avklaringen sin det",
      lopenummer: "2025/10002",
    },
    bruktBelop: 0,
    gjenstaendeBelop: 0,
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
  },
  {
    id: "17a7ff74-648b-4d43-a27d-d26cc2553b3b",
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
      lopenummer: "2025/10001",
    },
    bruktBelop: 55,
    gjenstaendeBelop: 0,
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
    bestillingsnummer: "A-2025/11073-2",
    beskrivelse: null,
  },
  {
    id: "27f81471-1c6a-4f68-921e-ba9da68d4e89",
    gjennomforing: {
      id: "6a760ab8-fb12-4c6e-b143-b711331f63f6",
      navn: "May rain - VTA",
      lopenummer: "2025/10003",
    },
    bruktBelop: 0,
    gjenstaendeBelop: 6000,
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
  },
  avklaringManedsprisTilsagn,
  arrUkesprisTilsagn,
];
