import { ArrangorInnsendingRadDto, ArrangorflateUtbetalingStatus, Valuta } from "api-client";
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

const arrUkesprisKlarTilGodkjenningTableRow: ArrangorInnsendingRadDto = {
  utbetalingId: arrUkesprisKlarTilGodkjenning.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  gjennomforingId: arrUkesprisKlarTilGodkjenning.gjennomforing.id,
  tiltakstypeNavn: "Arbeidsrettet rehabilitering",
  tiltakNavn: "Arbeidsrettet rehabilitering",
  lopenummer: "2025/10000",
  startDato: "2025-10-01",
  sluttDato: "2025-11-01",
  belop: { belop: 53100, valuta: Valuta.NOK },
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
};

const avklaringManedKlarTilInnsendingTableRow: ArrangorInnsendingRadDto = {
  utbetalingId: avklaringManedKlarTilGodkjenning.id,
  gjennomforingId: avklaringManedKlarTilGodkjenning.gjennomforing.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Avklaring",
  tiltakNavn: "Avklaring",
  lopenummer: "2025/10001",
  startDato: "2025-10-01",
  sluttDato: "2025-11-06",
  belop: { belop: 20000, valuta: Valuta.NOK },
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
};

const solrikAftDataRow: ArrangorInnsendingRadDto = {
  utbetalingId: aftKreverEndring.id,
  gjennomforingId: aftKreverEndring.gjennomforing.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Arbeidsforberedende trening",
  tiltakNavn: "Arbeidsforberedende trening",
  lopenummer: "2025/10003",
  startDato: "2025-01-01",
  sluttDato: "2025-02-01",
  belop: { belop: 242904, valuta: Valuta.NOK },
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.UBEHANDLET_FORSLAG,
};

const aftTiltakspengerTableRow: ArrangorInnsendingRadDto = {
  gjennomforingId: aftBehandlesAvNav.gjennomforing.id,
  utbetalingId: aftBehandlesAvNav.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Arbeidsforberedende trening",
  tiltakNavn: "Arbeidsforberedende trening",
  lopenummer: "2025/10004",
  startDato: "2025-05-01",
  sluttDato: "2025-08-02",
  belop: { belop: 234, valuta: Valuta.NOK },
  type: "INVESTERING",
  status: ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
};

const aftTiltakspengerOverfortTilUtbetalingTableRow: ArrangorInnsendingRadDto = {
  gjennomforingId: avklaringOverfortTilUtbetaling.gjennomforing.id,
  utbetalingId: avklaringOverfortTilUtbetaling.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Arbeidsforberedende trening",
  tiltakNavn: "Arbeidsforberedende trening",
  lopenummer: "2025/10005",
  startDato: "2025-01-01",
  sluttDato: "2025-02-01",
  belop: { belop: 1200, valuta: Valuta.NOK },
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
};

const mayRainAftTableRow: ArrangorInnsendingRadDto = {
  gjennomforingId: vtaKlarForGodkjenning.gjennomforing.id,
  utbetalingId: vtaKlarForGodkjenning.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Varig tilrettelagt arbeid i skjermet virksomhet",
  tiltakNavn: "Varig tilrettelagt arbeid i skjermet virksomhet",
  lopenummer: "2025/10006",
  startDato: "2025-06-01",
  sluttDato: "2025-07-01",
  belop: { belop: 16848, valuta: Valuta.NOK },
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
};

const aftFoobarTableRow: ArrangorInnsendingRadDto = {
  gjennomforingId: aftUtbetalt.gjennomforing.id,
  utbetalingId: aftUtbetalt.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Arbeidsforberedende trening",
  tiltakNavn: "Arbeidsforberedende trening",
  lopenummer: "2025/10002",
  startDato: "2025-01-01",
  sluttDato: "2025-02-01",
  belop: { belop: 1000, valuta: Valuta.NOK },
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.UTBETALT,
};

export const utbetalingTabellOversiktAktive: ArrangorInnsendingRadDto[] = [
  solrikAftDataRow,
  aftTiltakspengerTableRow,
  mayRainAftTableRow,
  avklaringManedKlarTilInnsendingTableRow,
  arrUkesprisKlarTilGodkjenningTableRow,
];

export const utbetalingTabellOversiktHistoriske: ArrangorInnsendingRadDto[] = [
  aftFoobarTableRow,
  aftTiltakspengerOverfortTilUtbetalingTableRow,
];
