import { Heading, InlineMessage, Box, VStack, BodyShort } from "@navikt/ds-react";
import { DataGrid } from "@navikt/ds-react/PREVIEW";
import { ArrangorflateTilsagnDto } from "@arrangor-utbetalinger/api-client";
import { tekster } from "../../tekster";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { formaterValuta, formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { SortComparators, useDataGridSort } from "~/hooks/useDataGridSort";

const collator = new Intl.Collator("nb", {
  numeric: true,
});

const sortComparators: SortComparators<ArrangorflateTilsagnDto> = {
  type: (a, b) => {
    const typeComparison = collator.compare(
      tekster.bokmal.tilsagn.tilsagntype(a.type),
      tekster.bokmal.tilsagn.tilsagntype(b.type),
    );

    return typeComparison || collator.compare(a.bestillingsnummer, b.bestillingsnummer);
  },

  periode: (a, b) =>
    a.periode.start.localeCompare(b.periode.start) ||
    a.periode.slutt.localeCompare(b.periode.slutt),

  totalbelop: (a, b) => {
    const aTotal = a.gjenstaendeBelop.belop + a.bruktBelop.belop;
    const bTotal = b.gjenstaendeBelop.belop + b.bruktBelop.belop;

    return aTotal - bTotal;
  },

  gjenstaendeBelop: (a, b) => a.gjenstaendeBelop.belop - b.gjenstaendeBelop.belop,

  deltakere: (a, b) => {
    const commonLength = Math.min(a.deltakere.length, b.deltakere.length);

    for (let index = 0; index < commonLength; index++) {
      const aDeltaker = a.deltakere[index];
      const bDeltaker = b.deltakere[index];

      const comparison =
        collator.compare(aDeltaker.navn ?? "", bDeltaker.navn ?? "") ||
        collator.compare(aDeltaker.norskIdent ?? "", bDeltaker.norskIdent ?? "");

      if (comparison !== 0) {
        return comparison;
      }
    }

    return a.deltakere.length - b.deltakere.length;
  },
};

const columns: DataGrid.Columns<ArrangorflateTilsagnDto> = [
  {
    id: "deltakere",
    header: "Deltakere",
    bodyCell: ({ deltakere }) => (
      <ul>
        {deltakere.map((d, index) => {
          return (
            <li key={index}>
              <VStack>
                <BodyShort>{d.navn}</BodyShort>
                <BodyShort size="small">{d.norskIdent ?? "-"}</BodyShort>
              </VStack>
            </li>
          );
        })}
      </ul>
    ),
    width: { autoResizeOnce: true },
    isSortable: true,
  },
  {
    id: "type",
    isRowHeader: true,
    header: "Type",
    bodyCell: ({ type, bestillingsnummer }) => (
      <VStack>
        <BodyShort>{tekster.bokmal.tilsagn.tilsagntype(type)}</BodyShort>
        <BodyShort size="small">{bestillingsnummer}</BodyShort>
      </VStack>
    ),
    width: { autoResizeOnce: true },
    isSortable: true,
  },
  {
    id: "periode",
    header: "Periode",
    bodyCell: ({ periode }) => formaterPeriode(periode),
    width: { autoResizeOnce: true },
    isSortable: true,
  },
  {
    id: "totalbelop",
    header: "Totalbeløp",
    bodyCell: ({ bruktBelop, gjenstaendeBelop }) =>
      formaterValuta(gjenstaendeBelop.belop + bruktBelop.belop, bruktBelop.valuta),
    width: { autoResizeOnce: true },
    isSortable: true,
    align: "right",
  },
  {
    id: "gjenstaendeBelop",
    header: "Gjenstaende beløp",
    bodyCell: ({ gjenstaendeBelop }) => formaterValutaBelop(gjenstaendeBelop),
    width: { autoResizeOnce: true },
    isSortable: true,
    align: "right",
  },
];

export function TilgjengeligeTilsagnDataGrid({ tilsagn }: { tilsagn: ArrangorflateTilsagnDto[] }) {
  const { sortedData, sortState, handleSortOrderChange } = useDataGridSort<ArrangorflateTilsagnDto>(
    tilsagn,
    sortComparators,
    [
      {
        columnId: "deltakere",
        direction: "asc",
      },
    ],
  );

  return (
    <Box>
      <Heading level="3" size="medium" spacing>
        Tilgjengelige tilsagn
      </Heading>
      <VStack gap="space-16">
        <InlineMessage status="info">
          Under vises informasjon om antatt forbruk. Hva som blir utbetalt avhenger imidlertid av
          faktisk forbruk i perioden.
        </InlineMessage>
        <DataGrid columns={columns} data={sortedData} getRowId={(row) => row.id}>
          <DataGrid.Table
            layout="auto"
            sorting={{
              allowMultiSort: true,
              sortOrder: sortState,
              onSortOrderChange: handleSortOrderChange,
            }}
          />
        </DataGrid>
      </VStack>
    </Box>
  );
}
