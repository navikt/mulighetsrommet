import {
  ArrangorflateTilsagnDto,
  DataElementTextFormat,
  LabeledDataElementType,
  TilsagnStatus,
  TilsagnType,
  Tiltakskode,
} from "api-client";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";

const beregning = {
  header: null,
  entries: [
    {
      label: "Tilsagnsperiode",
      type: LabeledDataElementType.INLINE,
      value: { value: "01.10.2025 - 06.11.2025", format: null },
    },
    {
      label: "Antall plasser",
      type: LabeledDataElementType.INLINE,
      value: { value: "100", format: DataElementTextFormat.NUMBER },
    },
    {
      label: "Avtalt månedspris per tiltaksplass",
      type: LabeledDataElementType.INLINE,
      value: { value: "10000", format: DataElementTextFormat.NOK },
    },
    {
      label: "Totalbeløp",
      type: LabeledDataElementType.INLINE,
      value: { value: "1200000", format: DataElementTextFormat.NOK },
    },
    {
      label: "Gjenstående beløp",
      type: LabeledDataElementType.INLINE,
      value: { value: "1200000", format: DataElementTextFormat.NOK },
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
  },
  arrangor: arrangorMock,
  type: TilsagnType.TILSAGN,
  periode: { start: "2025-10-01", slutt: "2025-11-07" },
  status: TilsagnStatus.GODKJENT,
  bruktBelop: 0,
  gjenstaendeBelop: 1200000,
  beregning,
  bestillingsnummer: "A-2025/12611-1",
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
  },
  arrangor: arrangorMock,
  type: TilsagnType.TILSAGN,
  periode: { start: "2025-10-01", slutt: "2025-11-01" },
  status: TilsagnStatus.GODKJENT,
  bruktBelop: 0,
  gjenstaendeBelop: 248400,
  beregning,
  bestillingsnummer: "A-2025/4123-1",
};

export const arrangorflateTilsagn: ArrangorflateTilsagnDto[] = [
  {
    id: "ad77762c-eebb-4623-be6d-0c64da79f2dd",
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
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
    beregning,
    arrangor: arrangorMock,
    status: TilsagnStatus.GODKJENT,
    bestillingsnummer: "A-2025/11073-1",
  },
  {
    id: "d8ccb57f-b9db-48e1-97f1-cb38426a9389",
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
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
    beregning,
    arrangor: arrangorMock,
    status: TilsagnStatus.GODKJENT,
    bestillingsnummer: "A-2025/11073-2",
  },
  {
    id: "f8fbc0f7-3280-410b-8387-20ff63896926",
    gjennomforing: arrangorMock,
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
    beregning,
    arrangor: arrangorMock,
    status: TilsagnStatus.ANNULLERT,
    bestillingsnummer: "A-2025/11147-2",
  },
  {
    id: "17a7ff74-648b-4d43-a27d-d26cc2553b3b",
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
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
    beregning,
    arrangor: arrangorMock,
    status: TilsagnStatus.OPPGJORT,
    bestillingsnummer: "A-2025/11073-2",
  },
  {
    id: "27f81471-1c6a-4f68-921e-ba9da68d4e89",
    gjennomforing: { id: "6a760ab8-fb12-4c6e-b143-b711331f63f6", navn: "May rain - VTA " },
    bruktBelop: 0,
    gjenstaendeBelop: 6000,
    tiltakstype: {
      navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
      tiltakskode: Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    },
    type: TilsagnType.TILSAGN,
    periode: { start: "2025-04-01", slutt: "2025-10-01" },
    beregning,
    arrangor: arrangorMock,
    status: TilsagnStatus.GODKJENT,
    bestillingsnummer: "A-2025/11398-1",
  },
  avklaringManedsprisTilsagn,
  arrUkesprisTilsagn,
];
