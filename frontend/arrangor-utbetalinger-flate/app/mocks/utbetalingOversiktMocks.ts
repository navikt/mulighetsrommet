import {
  ArrangorflateUtbetalingRadDto,
  ArrangorflateUtbetalingStatus,
  PaginatedResponseArrangorflateUtbetalingRadDto,
  Tiltakskode,
  Valuta,
} from "@arrangor-utbetalinger/api-client";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";
import {
  arrUkesprisKlarTilGodkjenning,
  avklaringManedKlarTilGodkjenning,
  avklaringOverfortTilUtbetaling,
  vtaKlarForGodkjenning,
  aftKreverEndring,
  aftBehandlesAvNav,
  aftUtbetalt,
} from "./utbetalingDetaljerMocks";

const arrUkesprisKlarTilGodkjenningTableRow: ArrangorflateUtbetalingRadDto = {
  utbetalingId: arrUkesprisKlarTilGodkjenning.id,
  gjennomforing: arrUkesprisKlarTilGodkjenning.gjennomforing,
  arrangor: arrangorMock,
  tiltakstype: {
    navn: "Arbeidsrettet rehabilitering",
    tiltakskode: Tiltakskode.ARBEIDSRETTET_REHABILITERING,
  },
  periode: { start: "2025-10-01", slutt: "2025-11-01" },
  beregnetBelop: { belop: 53100, valuta: Valuta.NOK },
  godkjentBelop: null,
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
};

const avklaringManedKlarTilInnsendingTableRow: ArrangorflateUtbetalingRadDto = {
  utbetalingId: avklaringManedKlarTilGodkjenning.id,
  gjennomforing: avklaringManedKlarTilGodkjenning.gjennomforing,
  arrangor: arrangorMock,
  tiltakstype: { navn: "Avklaring", tiltakskode: Tiltakskode.AVKLARING },
  periode: { start: "2025-10-01", slutt: "2025-11-06" },
  beregnetBelop: { belop: 20000, valuta: Valuta.NOK },
  godkjentBelop: null,
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
};

const solrikAftDataRow: ArrangorflateUtbetalingRadDto = {
  utbetalingId: aftKreverEndring.id,
  gjennomforing: aftKreverEndring.gjennomforing,
  arrangor: arrangorMock,
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  periode: { start: "2025-01-01", slutt: "2025-02-01" },
  beregnetBelop: { belop: 242904, valuta: Valuta.NOK },
  godkjentBelop: null,
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.BLOKKERT_FOR_INNSENDING,
};

const aftTiltakspengerTableRow: ArrangorflateUtbetalingRadDto = {
  utbetalingId: aftBehandlesAvNav.id,
  gjennomforing: aftBehandlesAvNav.gjennomforing,
  arrangor: arrangorMock,
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  periode: { start: "2025-05-01", slutt: "2025-08-02" },
  beregnetBelop: { belop: 234, valuta: Valuta.NOK },
  godkjentBelop: null,
  type: "INVESTERING",
  status: ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
};

const aftTiltakspengerOverfortTilUtbetalingTableRow: ArrangorflateUtbetalingRadDto = {
  utbetalingId: avklaringOverfortTilUtbetaling.id,
  gjennomforing: avklaringOverfortTilUtbetaling.gjennomforing,
  arrangor: arrangorMock,
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  periode: { start: "2025-01-01", slutt: "2025-02-01" },
  beregnetBelop: { belop: 1200, valuta: Valuta.NOK },
  godkjentBelop: { belop: 1200, valuta: Valuta.NOK },
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
};

const mayRainAftTableRow: ArrangorflateUtbetalingRadDto = {
  utbetalingId: vtaKlarForGodkjenning.id,
  gjennomforing: vtaKlarForGodkjenning.gjennomforing,
  arrangor: arrangorMock,
  tiltakstype: {
    navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
    tiltakskode: Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
  },
  periode: { start: "2025-06-01", slutt: "2025-07-01" },
  beregnetBelop: { belop: 16848, valuta: Valuta.NOK },
  godkjentBelop: null,
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
};

const aftFoobarTableRow: ArrangorflateUtbetalingRadDto = {
  utbetalingId: aftUtbetalt.id,
  gjennomforing: aftUtbetalt.gjennomforing,
  arrangor: arrangorMock,
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  periode: { start: "2025-01-01", slutt: "2025-02-01" },
  beregnetBelop: { belop: 1000, valuta: Valuta.NOK },
  godkjentBelop: { belop: 1000, valuta: Valuta.NOK },
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.UTBETALT,
};

const utbetalingTabellOversiktAktiveRader: ArrangorflateUtbetalingRadDto[] = [
  solrikAftDataRow,
  aftTiltakspengerTableRow,
  mayRainAftTableRow,
  avklaringManedKlarTilInnsendingTableRow,
  arrUkesprisKlarTilGodkjenningTableRow,
];

export const utbetalingTabellOversiktAktive: PaginatedResponseArrangorflateUtbetalingRadDto = {
  pagination: {
    totalCount: 5,
    pageSize: 20,
    totalPages: 1,
  },
  data: utbetalingTabellOversiktAktiveRader,
};

const utbetalingTabellOversiktHistoriskeRader: ArrangorflateUtbetalingRadDto[] = [
  aftFoobarTableRow,
  aftTiltakspengerOverfortTilUtbetalingTableRow,
];

export const utbetalingTabellOversiktHistoriske: PaginatedResponseArrangorflateUtbetalingRadDto = {
  pagination: {
    totalCount: 2,
    pageSize: 20,
    totalPages: 1,
  },
  data: utbetalingTabellOversiktHistoriskeRader,
};
