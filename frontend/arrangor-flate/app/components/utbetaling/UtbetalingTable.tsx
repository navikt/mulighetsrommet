import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, BodyShort, HelpText, HStack, Table } from "@navikt/ds-react";
import { ArrangorflateUtbetalingKompaktDto } from "api-client";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";
import { UtbetalingTextLink } from "./UtbetalingTextLink";
import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { useOrgnrFromUrl } from "~/utils/navigation";
import { sortBy, SortBySelector } from "~/utils/sort-by";
import { useSortState } from "~/hooks/useSortState";
import { formaterPeriode } from "@mr/frontend-common/utils/date";

interface Props {
  utbetalinger: ArrangorflateUtbetalingKompaktDto[];
  belopColumn: "godkjent" | "innsendt";
}

enum UtbetalingSortKey {
  BELOP = "BELOP",
  GODKJENT_BELOP = "GODKJENT_BELOP",
  PERIODE = "PERIODE",
  STATUS = "STATUS",
}

function getUtbetalingSelector(
  sortKey: UtbetalingSortKey,
): SortBySelector<ArrangorflateUtbetalingKompaktDto> {
  switch (sortKey) {
    case UtbetalingSortKey.BELOP:
      return (u) => u.belop;
    case UtbetalingSortKey.PERIODE:
      return (u) => u.periode.start;
    case UtbetalingSortKey.STATUS:
      return (u) => u.status;
    case UtbetalingSortKey.GODKJENT_BELOP:
      return (u) => u.godkjentBelop;
  }
}

export function UtbetalingTable({ utbetalinger, belopColumn }: Props) {
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
          <Table.ColumnHeader
            align="right"
            scope="col"
            sortable
            sortKey={
              belopColumn === "innsendt"
                ? UtbetalingSortKey.BELOP
                : UtbetalingSortKey.GODKJENT_BELOP
            }
          >
            {belopColumn === "innsendt" ? "Beløp" : "Godkjent beløp"}
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" align="left" aria-label="Type">
            <HStack gap="2" wrap={false}>
              Type
              <HelpText title="Hva betyr forkortelsene?">
                <BodyShort>
                  <b>KOR:</b> Korrigering opprettet av NAV
                </BodyShort>
                <BodyShort>
                  <b>INV:</b> Utbetaling for investering
                </BodyShort>
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
          <Table.ColumnHeader scope="col">Handlinger</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map(
          ({ id, status, belop, godkjentBelop, periode, gjennomforing, tiltakstype, type }) => {
            const vistBelop = belopColumn === "innsendt" ? belop : godkjentBelop;

            return (
              <Table.Row key={id}>
                <Table.HeaderCell scope="row">{gjennomforing.navn}</Table.HeaderCell>
                <Table.DataCell className="whitespace-nowrap">
                  {formaterPeriode(periode)}
                </Table.DataCell>
                <Table.DataCell>{tiltakstype.navn}</Table.DataCell>
                <Table.DataCell align="right" className="whitespace-nowrap">
                  {vistBelop ? formaterNOK(vistBelop) : "-"}
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
          },
        )}
      </Table.Body>
    </Table>
  );
}
