import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, HelpText, HStack, Table } from "@navikt/ds-react";
import { ArrFlateUtbetalingKompakt } from "api-client";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";
import { UtbetalingTextLink } from "./UtbetalingTextLink";
import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { formaterPeriode } from "~/utils/date";
import { useOrgnrFromUrl } from "~/utils/navigation";
import { sortBy, SortBySelector, useSortState } from "~/utils/sort-by";

interface Props {
  utbetalinger: ArrFlateUtbetalingKompakt[];
}

enum UtbetalingSortKey {
  BELOP = "BELOP",
  PERIODE = "PERIODE",
  STATUS = "STATUS",
}

function getUtbetalingSelector(
  sortKey: UtbetalingSortKey,
): SortBySelector<ArrFlateUtbetalingKompakt> {
  switch (sortKey) {
    case UtbetalingSortKey.BELOP:
      return (u) => u.belop;
    case UtbetalingSortKey.PERIODE:
      return (u) => u.periode.start;
    case UtbetalingSortKey.STATUS:
      return (u) => u.status;
  }
}

export function UtbetalingTable({ utbetalinger }: Props) {
  const orgnr = useOrgnrFromUrl();

  const { sort, handleSort } = useSortState<UtbetalingSortKey>();

  const sortedData = sort
    ? sortBy(utbetalinger, sort.direction, getUtbetalingSelector(sort.orderBy))
    : utbetalinger;

  if (utbetalinger.length === 0) {
    return (
      <Alert className="mt-10" variant="info">
        Det finnes ingen utbetalinger her
      </Alert>
    );
  }

  return (
    <Table
      aria-describedby="innsending-table-header"
      zebraStripes
      sort={sort}
      onSortChange={(sortKey) => handleSort(sortKey as UtbetalingSortKey)}
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortable sortKey={UtbetalingSortKey.PERIODE}>
            Periode
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Tiltakstype</Table.ColumnHeader>
          <Table.ColumnHeader align="right" scope="col" sortable sortKey={UtbetalingSortKey.BELOP}>
            Beløp
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" align="left" aria-label="Type">
            <HStack gap="2" wrap={false}>
              Type
              <HelpText title="Hva betyr forkortelsene?">
                <p>
                  <b>KOR:</b> Korrigering opprettet av NAV
                </p>
                <p>
                  <b>INV:</b> Utbetaling for investering
                </p>
              </HelpText>
            </HStack>
          </Table.ColumnHeader>
          <Table.ColumnHeader
            scope="col"
            className="min-w-50"
            sortable
            sortKey={UtbetalingSortKey.STATUS}
          >
            Status
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" aria-label="Handlinger"></Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map(({ id, status, belop, periode, gjennomforing, tiltakstype, type }) => {
          return (
            <Table.Row key={id}>
              <Table.HeaderCell scope="row">{gjennomforing.navn}</Table.HeaderCell>
              <Table.DataCell className="whitespace-nowrap">
                {formaterPeriode(periode)}
              </Table.DataCell>
              <Table.DataCell>{tiltakstype.navn}</Table.DataCell>
              <Table.DataCell align="right" className="whitespace-nowrap">
                {formaterNOK(belop)}
              </Table.DataCell>
              <Table.DataCell>{type && <UtbetalingTypeTag type={type} />}</Table.DataCell>
              <Table.DataCell>
                <UtbetalingStatusTag status={status} />
              </Table.DataCell>
              <Table.DataCell>
                <UtbetalingTextLink
                  orgnr={orgnr}
                  status={status}
                  gjennomforingNavn={gjennomforing.navn}
                  utbetalingId={id}
                />
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
