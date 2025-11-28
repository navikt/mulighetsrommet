import {
  DataDrivenTableDto,
  DataDrivenTableDtoColumnAlign,
  DataElementTextFormat,
  TimelineDto,
  TimelineDtoRowPeriodVariant,
} from "@api-client";
import { dataElementText } from "./dataDrivenTableHelpers";
import { DataDrivenTableDtoRow } from "@mr/frontend-common/components/datadriven/types";

interface DeltakelseRow {
  cells: DeltakelseRowCell;
  content: TimelineDto | null;
}

interface DeltakelseRowCell {
  navn: string;
  identitetsnummer: string;
  tiltakStart: string;
  periodeStart: string;
  periodeSlutt: string;
  faktor: string;
}

function createDeltakelsesTable(
  faktorType: "Manedsverk" | "Ukesverk",
  rows: DeltakelseRow[],
): DataDrivenTableDto {
  return {
    columns: [
      { key: "navn", label: "Navn", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
      {
        key: "identitetsnummer",
        label: "Fødselsnr.",
        sortable: true,
        align: DataDrivenTableDtoColumnAlign.LEFT,
      },
      {
        key: "tiltakStart",
        label: "Startdato i tiltaket",
        sortable: true,
        align: DataDrivenTableDtoColumnAlign.LEFT,
      },
      {
        key: "periodeStart",
        label: "Startdato i perioden",
        sortable: true,
        align: DataDrivenTableDtoColumnAlign.LEFT,
      },
      {
        key: "periodeSlutt",
        label: "Sluttdato i perioden",
        sortable: true,
        align: DataDrivenTableDtoColumnAlign.LEFT,
      },
      {
        key: "faktor",
        label: faktorType,
        sortable: true,
        align: DataDrivenTableDtoColumnAlign.RIGHT,
      },
    ],
    rows: rows.map(({ cells, content }): DataDrivenTableDtoRow => {
      return {
        cells: {
          navn: dataElementText(cells.navn),
          identitetsnummer: dataElementText(cells.identitetsnummer),
          tiltakStart: dataElementText(cells.tiltakStart, DataElementTextFormat.DATE),
          periodeStart: dataElementText(cells.periodeStart, DataElementTextFormat.DATE),
          periodeSlutt: dataElementText(cells.periodeSlutt, DataElementTextFormat.DATE),
          faktor: dataElementText(cells.faktor, DataElementTextFormat.NUMBER),
        },
        content: content,
      };
    }),
  };
}

export const toSatserUkesverkDeltakelse = createDeltakelsesTable("Ukesverk", [
  {
    cells: {
      navn: "Nordmann, Ola",
      identitetsnummer: "27017809100",
      tiltakStart: "2025-07-29",
      periodeStart: "2025-09-29",
      periodeSlutt: "2025-10-27",
      faktor: "6.0",
    },
    content: {
      startDate: "2025-10-01",
      endDate: "2025-10-31",
      rows: [
        {
          periods: [
            {
              key: "0",
              start: "2025-09-29",
              end: "2025-10-14",
              status: TimelineDtoRowPeriodVariant.INFO,
              content: "Pris per uke: 777, Ukesverk: 3.0",
              hover: "Pris per uke: 777, Ukesverk: 3.0, Periode: 01.10.2025 - 31.10.2025",
            },
            {
              key: "1",
              start: "2025-10-15",
              end: "2025-10-27",
              status: TimelineDtoRowPeriodVariant.INFO,
              content: "Pris per uke: 888, Ukesverk: 3.0",
              hover: "Pris per uke: 888, Ukesverk: 3.0, Periode: 01.10.2025 - 31.10.2025",
            },
          ],
          label: "Beregning",
        },
      ],
    },
  },
]);

export const avklaringManedDeltakelse = createDeltakelsesTable("Manedsverk", [
  {
    cells: {
      navn: "Barberskum, Muskuløs",
      identitetsnummer: "21896896757",
      tiltakStart: "2025-09-01",
      periodeStart: "2025-10-01",
      periodeSlutt: "2025-11-01",
      faktor: "1.0",
    },
    content: null,
  },
  {
    cells: {
      navn: "Bjørk, Subtil",
      identitetsnummer: "06438749665",
      tiltakStart: "2025-09-15",
      periodeStart: "2025-10-01",
      periodeSlutt: "2025-11-01",
      faktor: "1.0",
    },
    content: null,
  },
]);

export const arrUkesverkDeltakelse = createDeltakelsesTable("Ukesverk", [
  {
    cells: {
      navn: "Barberskum, Muskuløs",
      identitetsnummer: "21896896757",
      tiltakStart: "2025-10-01",
      periodeStart: "2025-10-01",
      periodeSlutt: "2025-11-01",
      faktor: "4.0",
    },
    content: null,
  },
  {
    cells: {
      navn: "Bjørk, Subtil",
      identitetsnummer: "06438749665",
      tiltakStart: "2024-10-04",
      periodeStart: "2025-10-01",
      periodeSlutt: "2025-11-01",
      faktor: "3.8",
    },
    content: null,
  },
  {
    cells: {
      navn: "Krem, Kry",
      identitetsnummer: "16428500745",
      tiltakStart: "2025-09-01",
      periodeStart: "2025-10-01",
      periodeSlutt: "2025-11-01",
      faktor: "4",
    },
    content: null,
  },
]);

export const vtaManedDeltakelse = createDeltakelsesTable("Ukesverk", [
  {
    cells: {
      navn: "Barberskum, Muskuløs",
      identitetsnummer: "21896896757",
      tiltakStart: "2025-05-15",
      periodeStart: "2025-06-01",
      periodeSlutt: "2025-06-30",
      faktor: "0.8",
    },
    content: null,
  },
]);
