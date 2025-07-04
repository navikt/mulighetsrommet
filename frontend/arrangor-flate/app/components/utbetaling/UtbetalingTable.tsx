import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, HelpText, HStack, SortState, Table, VStack } from "@navikt/ds-react";
import React, { useState } from "react";
import { formaterPeriode, useOrgnrFromUrl } from "~/utils";
import { ArrFlateUtbetalingKompakt } from "api-client";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";
import { UtbetalingTextLink } from "./UtbetalingTextLink";
import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";

interface Props {
  utbetalinger: ArrFlateUtbetalingKompakt[];
}

interface ScopedSortState extends SortState {
  orderBy: keyof ArrFlateUtbetalingKompakt;
}

export function UtbetalingTable({ utbetalinger }: Props) {
  const orgnr = useOrgnrFromUrl();

  const [sort, setSort] = useState<ScopedSortState | undefined>();

  const handleSort = (sortKey: ScopedSortState["orderBy"]) => {
    setSort(
      sort && sortKey === sort.orderBy && sort.direction === "descending"
        ? undefined
        : {
            orderBy: sortKey,
            direction:
              sort && sortKey === sort.orderBy && sort.direction === "ascending"
                ? "descending"
                : "ascending",
          },
    );
  };

  function comparator<T>(a: T, b: T, orderBy: keyof T): number {
    if (b[orderBy] == null || b[orderBy] < a[orderBy]) {
      return -1;
    }
    if (b[orderBy] > a[orderBy]) {
      return 1;
    }
    return 0;
  }

  const sortedData = utbetalinger.slice().sort((a, b) => {
    if (sort) {
      return sort.direction === "ascending"
        ? comparator(b, a, sort.orderBy)
        : comparator(a, b, sort.orderBy);
    }
    return 1;
  });

  if (utbetalinger.length === 0) {
    return (
      <Alert className="my-10 mt-10" variant="info">
        Det finnes ingen utbetalinger her
      </Alert>
    );
  }

  const harUtbetalingsType = sortedData.some((utbetaling) => utbetaling.type);

  return (
    <Table
      aria-label="Utbetalinger"
      zebraStripes
      sort={sort}
      onSortChange={(sortKey) => handleSort(sortKey as ScopedSortState["orderBy"])}
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col">Tiltakstype</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Periode</Table.ColumnHeader>
          <Table.ColumnHeader
            align="right"
            scope="col"
            className="min-w-32"
            sortable
            sortKey="belop"
          >
            Beløp
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" className="min-w-44" sortable sortKey="status">
            Status
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" align="left" hidden={!harUtbetalingsType}>
            <HStack gap="2">
              Type
              <HelpText title="Hva betyr forkortelsene?">
                <VStack gap="1" className="text-left">
                  <div>
                    <b>KOR:</b> Korrigering opprettet av NAV
                  </div>
                  <div>
                    <b>INV:</b> Utbetaling for investering
                  </div>
                </VStack>
              </HelpText>
            </HStack>
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col"></Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map(({ id, status, belop, periode, gjennomforing, tiltakstype, type }) => {
          return (
            <React.Fragment key={id}>
              <Table.Row>
                <Table.DataCell>{tiltakstype.navn}</Table.DataCell>
                <Table.DataCell>{gjennomforing.navn}</Table.DataCell>
                <Table.DataCell>{formaterPeriode(periode)}</Table.DataCell>
                <Table.DataCell align="right">{formaterNOK(belop)}</Table.DataCell>
                <Table.DataCell>
                  <UtbetalingStatusTag status={status} />
                </Table.DataCell>
                {harUtbetalingsType && (
                  <Table.DataCell align="left">
                    {type && <UtbetalingTypeTag type={type} />}
                  </Table.DataCell>
                )}
                <Table.DataCell>
                  <UtbetalingTextLink
                    orgnr={orgnr}
                    status={status}
                    gjennomforingNavn={gjennomforing.navn}
                    utbetalingId={id}
                  />
                </Table.DataCell>
              </Table.Row>
            </React.Fragment>
          );
        })}
      </Table.Body>
    </Table>
  );
}
