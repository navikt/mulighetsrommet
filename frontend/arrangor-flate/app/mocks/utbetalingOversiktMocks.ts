import {
  ArrangorflateUtbetalingKompaktDto,
  ArrangorflateUtbetalingStatus,
  Tiltakskode,
} from "api-client";
import { utbetalingType } from "./utbetalingTypeMocks";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";

const arrUkesprisKlarTilGodkjenning: ArrangorflateUtbetalingKompaktDto = {
  id: "ba046f93-cb0c-4acf-a724-99a36481f183",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  godkjentAvArrangorTidspunkt: null,
  tiltakstype: {
    navn: "Arbeidsrettet rehabilitering",
    tiltakskode: Tiltakskode.ARBEIDSRETTET_REHABILITERING,
  },
  gjennomforing: {
    id: "a47092ba-410b-4ca1-9713-36506a039742",
    navn: "Arbeidsrettet rehabilitering - avtalt ukespris",
  },
  arrangor: arrangorMock,
  periode: { start: "2025-10-01", slutt: "2025-11-01" },
  type: { displayName: "Innsending", displayNameLong: null, tagName: null },
  belop: 53100,
  godkjentBelop: null,
};

const avklaringManedKlarTilInnsending: ArrangorflateUtbetalingKompaktDto = {
  id: "a134c0bf-40eb-4124-8f2e-df7b7c51fd44",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  godkjentAvArrangorTidspunkt: null,
  tiltakstype: { navn: "Avklaring", tiltakskode: Tiltakskode.AVKLARING },
  gjennomforing: {
    id: "70cdc182-8913-48c0-bad9-fa4e74f3288e",
    navn: "Avklaring - avtalt månedspris",
  },
  arrangor: arrangorMock,
  periode: { start: "2025-10-01", slutt: "2025-11-06" },
  type: utbetalingType.INNSENDING,
  belop: 20000,
  godkjentBelop: null,
};

export const mockArrangorflateUtbetalingKompakt: ArrangorflateUtbetalingKompaktDto[] = [
  {
    id: "e48f9b35-855f-43aa-8b4d-a669013df34b",
    status: ArrangorflateUtbetalingStatus.UTBETALT,
    godkjentAvArrangorTidspunkt: "2025-05-15T11:03:21.959059",
    godkjentBelop: 1000,
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    },
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
    },
    arrangor: arrangorMock,
    periode: {
      start: "2025-01-01",
      slutt: "2025-02-01",
    },
    belop: 10149,
    type: utbetalingType.INNSENDING,
  },
  {
    id: "a5499e34-9fb4-49d1-a37d-11810f6df19b",
    status: ArrangorflateUtbetalingStatus.KREVER_ENDRING,
    godkjentAvArrangorTidspunkt: null,
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    },
    gjennomforing: {
      id: "b3e1cfbb-bfb5-4b4b-b8a4-af837631ed51",
      navn: "Solrikt AFT",
    },
    arrangor: arrangorMock,
    periode: {
      start: "2025-01-01",
      slutt: "2025-02-01",
    },
    belop: 242904,
    godkjentBelop: null,
    type: utbetalingType.INNSENDING,
  },
  {
    id: "585a2834-338a-4ac7-82e0-e1b08bfe1408",
    status: ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
    godkjentAvArrangorTidspunkt: "2025-06-05T09:36:11.510229",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    },
    gjennomforing: {
      id: "d29cb67c-8e68-4ece-90dc-ff21c498aa3f",
      navn: "AFT - Arbeidsforberedende trening - Team tiltakspenger",
    },
    arrangor: arrangorMock,
    periode: {
      start: "2025-05-01",
      slutt: "2025-08-02",
    },
    belop: 234,
    godkjentBelop: null,
    type: utbetalingType.INVESTERING,
  },
  {
    id: "153bc6f0-0c5f-4555-9447-b88ea0cc60f2",
    status: ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
    godkjentBelop: 1200,
    godkjentAvArrangorTidspunkt: null,
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    },
    gjennomforing: {
      id: "6d71a9c5-c920-4d56-bc3b-2da07e4b6100",
      navn: "AFT - Arbeidsforberedende trening - Team tiltakspenger",
    },
    arrangor: arrangorMock,
    periode: {
      start: "2025-01-01",
      slutt: "2025-02-01",
    },
    belop: 500,
    type: utbetalingType.KORRIGERING,
  },
  {
    id: "fdbb7433-b42e-4cd6-b995-74a8e487329f",
    status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
    godkjentAvArrangorTidspunkt: null,
    tiltakstype: {
      navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    },
    gjennomforing: {
      id: "6a760ab8-fb12-4c6e-b143-b711331f63f6",
      navn: "May rain - VTA ",
    },
    arrangor: arrangorMock,
    periode: {
      start: "2025-06-01",
      slutt: "2025-07-01",
    },
    belop: 16848,
    godkjentBelop: null,
    type: utbetalingType.INNSENDING,
  },
  avklaringManedKlarTilInnsending,
  arrUkesprisKlarTilGodkjenning,
];
