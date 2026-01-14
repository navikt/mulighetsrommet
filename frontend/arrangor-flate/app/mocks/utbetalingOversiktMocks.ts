import {
  DataDrivenTableDto,
  DataDrivenTableDtoColumnAlign,
  DataDrivenTableDtoRow,
  DataElement,
  DataElementStatusVariant,
  DataElementTextFormat,
} from "api-client";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";
import {
  dataElementAction,
  dataElementPeriode,
  dataElementStatus,
  dataElementText,
} from "~/mocks/dataDrivenTableHelpers";
import { pathTo } from "~/utils/navigation";
import {
  aftBehandlesAvNav,
  aftKreverEndring,
  aftUtbetalt,
  arrUkesprisKlarTilGodkjenning,
  avklaringManedKlarTilGodkjenning,
  avklaringOverfortTilUtbetaling,
  vtaKlarForGodkjenning,
} from "./utbetalingDetaljerMocks";

const dataElementInvestering: DataElement = dataElementStatus(
  "INV",
  DataElementStatusVariant.NEUTRAL,
);

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
        avklaringManedKlarTilGodkjenning.id,
      ),
    ),
  },
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
      pathTo.beregning(arrangorMock.organisasjonsnummer, aftKreverEndring.id),
    ),
  },
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
      pathTo.detaljer(arrangorMock.organisasjonsnummer, aftBehandlesAvNav.id),
    ),
  },
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
      pathTo.detaljer(arrangorMock.organisasjonsnummer, avklaringOverfortTilUtbetaling.id),
    ),
  },
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
      pathTo.innsendingsinformasjon(arrangorMock.organisasjonsnummer, vtaKlarForGodkjenning.id),
    ),
  },
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
      pathTo.detaljer(arrangorMock.organisasjonsnummer, aftUtbetalt.id),
    ),
  },
};

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
