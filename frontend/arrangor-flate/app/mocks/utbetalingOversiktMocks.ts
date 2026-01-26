import { ArrangorInnsendingRadDto, ArrangorflateUtbetalingStatus } from "api-client";
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
  gjennomforingId: arrUkesprisKlarTilGodkjenning.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Arbeidsrettet rehabilitering",
  tiltakNavn: "Arbeidsrettet rehabilitering",
  lopenummer: "2025/10000",
  startDato: "2025-10-01",
  sluttDato: "2025-11-01",
  belop: 53100,
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
};

const avklaringManedKlarTilInnsendingTableRow: ArrangorInnsendingRadDto = {
  gjennomforingId: avklaringManedKlarTilGodkjenning.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Avklaring",
  tiltakNavn: "Avklaring",
  lopenummer: "2025/10001",
  startDato: "2025-10-01",
  sluttDato: "2025-11-06",
  belop: 20000,
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
};

const solrikAftDataRow: ArrangorInnsendingRadDto = {
  gjennomforingId: aftKreverEndring.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Arbeidsforberedende trening",
  tiltakNavn: "Arbeidsforberedende trening",
  lopenummer: "2025/10003",
  startDato: "2025-01-01",
  sluttDato: "2025-02-01",
  belop: 242904,
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.KREVER_ENDRING,
};

const aftTiltakspengerTableRow: ArrangorInnsendingRadDto = {
  gjennomforingId: aftBehandlesAvNav.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Arbeidsforberedende trening",
  tiltakNavn: "Arbeidsforberedende trening",
  lopenummer: "2025/10004",
  startDato: "2025-05-01",
  sluttDato: "2025-08-02",
  belop: 234,
  type: "INVESTERING",
  status: ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
};

const aftTiltakspengerOverfortTilUtbetalingTableRow: ArrangorInnsendingRadDto = {
  gjennomforingId: avklaringOverfortTilUtbetaling.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Arbeidsforberedende trening",
  tiltakNavn: "Arbeidsforberedende trening",
  lopenummer: "2025/10005",
  startDato: "2025-01-01",
  sluttDato: "2025-02-01",
  belop: 1200,
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
};

const mayRainAftTableRow: ArrangorInnsendingRadDto = {
  gjennomforingId: vtaKlarForGodkjenning.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Varig tilrettelagt arbeid i skjermet virksomhet",
  tiltakNavn: "Varig tilrettelagt arbeid i skjermet virksomhet",
  lopenummer: "2025/10006",
  startDato: "2025-06-01",
  sluttDato: "2025-07-01",
  belop: 16848,
  type: "INNSENDING",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
};

const aftFoobarTableRow: ArrangorInnsendingRadDto = {
  gjennomforingId: aftUtbetalt.id,
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  tiltakstypeNavn: "Arbeidsforberedende trening",
  tiltakNavn: "Arbeidsforberedende trening",
  lopenummer: "2025/10002",
  startDato: "2025-01-01",
  sluttDato: "2025-02-01",
  belop: 1000,
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
