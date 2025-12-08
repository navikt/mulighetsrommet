import {
  ArrangorflateUtbetalingKompaktDto,
  ArrangorflateUtbetalingStatus,
  DataDrivenTableDto,
  DataDrivenTableDtoColumnAlign,
  DataDrivenTableDtoRow,
  DataElement,
  DataElementStatusVariant,
  DataElementTextFormat,
  Tiltakskode,
} from "api-client";
import { utbetalingType } from "./utbetalingTypeMocks";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";
import {
  dataElementAction,
  dataElementPeriode,
  dataElementStatus,
  dataElementText,
} from "~/mocks/dataDrivenTableHelpers";
import { pathTo } from "~/utils/navigation";

const dataElementInvestering: DataElement = dataElementStatus(
  "INV",
  DataElementStatusVariant.NEUTRAL,
);

const arrUkesprisKlarTilGodkjenning: ArrangorflateUtbetalingKompaktDto = {
  id: "ba046f93-cb0c-4acf-a724-99a36481f183",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  tiltakstype: {
    navn: "Arbeidsrettet rehabilitering",
    tiltakskode: Tiltakskode.ARBEIDSRETTET_REHABILITERING,
  },
  gjennomforing: {
    id: "a47092ba-410b-4ca1-9713-36506a039742",
    navn: "Arbeidsrettet rehabilitering - avtalt ukespris",
    lopenummer: "2025/10000",
  },
  arrangor: arrangorMock,
  periode: { start: "2025-10-01", slutt: "2025-11-01" },
  type: { displayName: "Innsending", displayNameLong: null, tagName: null },
  belop: 53100,
  godkjentBelop: null,
};

const arrUkesprisKlarTilGodkjenningTableRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Arbeidsrettet rehabilitering (2025/10000)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({ start: "2025-10-01", slutt: "2025-11-01" }),
    belop: dataElementText("53100", DataElementTextFormat.NOK),
    type: dataElementText(""),
    status: dataElementStatus("Klar for innsending", DataElementStatusVariant.WARNING),
    action: dataElementAction(
      "Start innsending",
      pathTo.innsendingsinformasjon(
        arrangorMock.organisasjonsnummer,
        arrUkesprisKlarTilGodkjenning.id,
      ),
    ),
  },
};

const avklaringManedKlarTilInnsending: ArrangorflateUtbetalingKompaktDto = {
  id: "a134c0bf-40eb-4124-8f2e-df7b7c51fd44",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  tiltakstype: { navn: "Avklaring", tiltakskode: Tiltakskode.AVKLARING },
  gjennomforing: {
    id: "70cdc182-8913-48c0-bad9-fa4e74f3288e",
    navn: "Avklaring - avtalt månedspris",
    lopenummer: "2025/10001",
  },
  arrangor: arrangorMock,
  periode: { start: "2025-10-01", slutt: "2025-11-06" },
  type: utbetalingType.INNSENDING,
  belop: 20000,
  godkjentBelop: null,
};

const avklaringManedKlarTilInnsendingTableRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Avklaring (2025/10001)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({ start: "2025-10-01", slutt: "2025-11-06" }),
    belop: dataElementText("20000", DataElementTextFormat.NOK),
    type: dataElementText(""),
    status: dataElementStatus("Klar for innsending", DataElementStatusVariant.WARNING),
    action: dataElementAction(
      "Start innsending",
      pathTo.innsendingsinformasjon(
        arrangorMock.organisasjonsnummer,
        avklaringManedKlarTilInnsending.id,
      ),
    ),
  },
};

const solrikUtbetaling: ArrangorflateUtbetalingKompaktDto = {
  id: "a5499e34-9fb4-49d1-a37d-11810f6df19b",
  status: ArrangorflateUtbetalingStatus.KREVER_ENDRING,
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  gjennomforing: {
    id: "b3e1cfbb-bfb5-4b4b-b8a4-af837631ed51",
    navn: "Solrikt AFT",
    lopenummer: "2025/10003",
  },
  arrangor: arrangorMock,
  periode: {
    start: "2025-01-01",
    slutt: "2025-02-01",
  },
  belop: 242904,
  godkjentBelop: null,
  type: utbetalingType.INNSENDING,
};

const solrikAftDataRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Arbeidsforberedende trening (2025/10003)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({
      start: "2025-01-01",
      slutt: "2025-02-01",
    }),
    belop: dataElementText("242904", DataElementTextFormat.NOK),
    type: dataElementText(""),
    status: dataElementStatus("Krever endring", DataElementStatusVariant.WARNING),
    action: dataElementAction(
      "Se innsending",
      pathTo.beregning(arrangorMock.organisasjonsnummer, solrikUtbetaling.id),
    ),
  },
};

const aftTiltakspenger: ArrangorflateUtbetalingKompaktDto = {
  id: "585a2834-338a-4ac7-82e0-e1b08bfe1408",
  status: ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  gjennomforing: {
    id: "d29cb67c-8e68-4ece-90dc-ff21c498aa3f",
    navn: "AFT - Arbeidsforberedende trening - Team tiltakspenger",
    lopenummer: "2025/10004",
  },
  arrangor: arrangorMock,
  periode: {
    start: "2025-05-01",
    slutt: "2025-08-02",
  },
  belop: 234,
  godkjentBelop: null,
  type: utbetalingType.INVESTERING,
};

const aftTiltakspengerTableRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Arbeidsforberedende trening (2025/10004)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({
      start: "2025-05-01",
      slutt: "2025-08-02",
    }),
    belop: dataElementText("234", DataElementTextFormat.NOK),
    type: dataElementInvestering,
    status: dataElementStatus("Behandles av Nav", DataElementStatusVariant.WARNING),
    action: dataElementAction(
      "Se detaljer",
      pathTo.detaljer(arrangorMock.organisasjonsnummer, aftTiltakspenger.id),
    ),
  },
};

const aftTiltakspengerOverfortTilUtbetaling: ArrangorflateUtbetalingKompaktDto = {
  id: "153bc6f0-0c5f-4555-9447-b88ea0cc60f2",
  status: ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
  godkjentBelop: 1200,
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  gjennomforing: {
    id: "6d71a9c5-c920-4d56-bc3b-2da07e4b6100",
    navn: "AFT - Arbeidsforberedende trening - Team tiltakspenger",
    lopenummer: "2025/10005",
  },
  arrangor: arrangorMock,
  periode: {
    start: "2025-01-01",
    slutt: "2025-02-01",
  },
  belop: 500,
  type: utbetalingType.KORRIGERING,
};
const aftTiltakspengerOverfortTilUtbetalingTableRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Arbeidsforberedende trening (2025/10005)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({
      start: "2025-01-01",
      slutt: "2025-02-01",
    }),
    belop: dataElementText("1200", DataElementTextFormat.NOK),
    type: dataElementText(""),
    status: dataElementStatus("Overført til utbetaling", DataElementStatusVariant.SUCCESS),
    action: dataElementAction(
      "Se detaljer",
      pathTo.detaljer(arrangorMock.organisasjonsnummer, aftTiltakspengerOverfortTilUtbetaling.id),
    ),
  },
};
const mayRainAft: ArrangorflateUtbetalingKompaktDto = {
  id: "fdbb7433-b42e-4cd6-b995-74a8e487329f",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  tiltakstype: {
    navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
    tiltakskode: Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
  },
  gjennomforing: {
    id: "6a760ab8-fb12-4c6e-b143-b711331f63f6",
    navn: "May rain - VTA",
    lopenummer: "2025/10006",
  },
  arrangor: arrangorMock,
  periode: {
    start: "2025-06-01",
    slutt: "2025-07-01",
  },
  belop: 16848,
  godkjentBelop: null,
  type: utbetalingType.INNSENDING,
};

const mayRainAftTableRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Varig tilrettelagt arbeid i skjermet virksomhet (2025/10006)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({
      start: "2025-06-01",
      slutt: "2025-07-01",
    }),
    belop: dataElementText("16848", DataElementTextFormat.NOK),
    type: dataElementText(""),
    status: dataElementStatus("Klar for innsending", DataElementStatusVariant.ALT_1),
    action: dataElementAction(
      "Start innsending",
      pathTo.innsendingsinformasjon(arrangorMock.organisasjonsnummer, mayRainAft.id),
    ),
  },
};

const aftFoobar: ArrangorflateUtbetalingKompaktDto = {
  id: "e48f9b35-855f-43aa-8b4d-a669013df34b",
  status: ArrangorflateUtbetalingStatus.UTBETALT,
  godkjentBelop: 1000,
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  gjennomforing: {
    id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
    navn: "AFT Foobar",
    lopenummer: "2025/10002",
  },
  arrangor: arrangorMock,
  periode: {
    start: "2025-01-01",
    slutt: "2025-02-01",
  },
  belop: 10149,
  type: utbetalingType.INNSENDING,
};

const aftFoobarTableRow: DataDrivenTableDtoRow = {
  content: null,
  cells: {
    tiltak: dataElementText("Arbeidsforberedende trening (2025/10002)"),
    arrangor: dataElementText(`${arrangorMock.navn} (${arrangorMock.organisasjonsnummer})`),
    periode: dataElementPeriode({
      start: "2025-01-01",
      slutt: "2025-02-01",
    }),
    belop: dataElementText("1000", DataElementTextFormat.NOK),
    type: dataElementText(""),
    status: dataElementStatus("Utbetalt", DataElementStatusVariant.SUCCESS),
    action: dataElementAction(
      "Se detaljer",
      pathTo.detaljer(arrangorMock.organisasjonsnummer, aftFoobar.id),
    ),
  },
};

export const mockArrangorflateUtbetalingKompakt: ArrangorflateUtbetalingKompaktDto[] = [
  aftFoobar,
  solrikUtbetaling,
  aftTiltakspenger,
  aftTiltakspengerOverfortTilUtbetaling,
  avklaringManedKlarTilInnsending,
  arrUkesprisKlarTilGodkjenning,
];

export const utbetalingTabellOversiktAktive: DataDrivenTableDto = {
  columns: [
    { key: "tiltak", label: "Tiltak", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
    {
      key: "arrangor",
      label: "Arrangør",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    { key: "periode", label: "Periode", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
    { key: "belop", label: "Beløp", sortable: true, align: DataDrivenTableDtoColumnAlign.RIGHT },
    { key: "type", label: "Type", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
    { key: "status", label: "Status", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
    {
      key: "action",
      label: "Handlinger",
      sortable: false,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
  ],
  rows: [
    solrikAftDataRow,
    aftTiltakspengerTableRow,
    mayRainAftTableRow,
    avklaringManedKlarTilInnsendingTableRow,
    arrUkesprisKlarTilGodkjenningTableRow,
  ],
};

export const utbetalingTabellOversiktHistoriske: DataDrivenTableDto = {
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
      key: "belop",
      label: "Godkjent beløp",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.RIGHT,
    },
    { key: "type", label: "Type", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
    { key: "status", label: "Status", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
    {
      key: "action",
      label: "Handlinger",
      sortable: false,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
  ],
  rows: [aftFoobarTableRow, aftTiltakspengerOverfortTilUtbetalingTableRow],
};
