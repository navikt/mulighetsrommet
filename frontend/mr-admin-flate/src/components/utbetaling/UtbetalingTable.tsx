import {
  compare,
  compareByKey,
  formaterNavEnheter,
  formaterPeriodeSlutt,
  formaterPeriodeStart,
} from "@/utils/Utils";
import { AdminUtbetalingStatus, UtbetalingKompaktDto } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { HelpText, HStack, SortState, Table, VStack } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { useState } from "react";
import { Link, useParams } from "react-router";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";

interface Props {
  utbetalinger: UtbetalingKompaktDto[];
}

interface ScopedSortState extends SortState {
  orderBy: keyof UtbetalingKompaktDto;
}

interface UtbetalingTabellData extends UtbetalingKompaktDto {
  periodeStart: string;
  periodeSlutt: string;
  status: AdminUtbetalingStatus;
}

export function UtbetalingTable({ utbetalinger }: Props) {
  const { gjennomforingId } = useParams();

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

  const sortedData: UtbetalingTabellData[] = [...utbetalinger]
    .map((utbetaling) => ({
      ...utbetaling,
      periodeStart: utbetaling.periode.start,
      periodeSlutt: utbetaling.periode.slutt,
      status: utbetaling.status,
    }))
    .toSorted((a, b) => {
      if (sort) {
        if (sort.orderBy === "kostnadssteder") {
          return sort.direction === "ascending"
            ? compare(b.kostnadssteder.at(0)?.navn, a.kostnadssteder.at(0)?.navn)
            : compare(a.kostnadssteder.at(0)?.navn, b.kostnadssteder.at(0)?.navn);
        }
        return sort.direction === "ascending"
          ? compareByKey(b, a, sort.orderBy)
          : compareByKey(a, b, sort.orderBy);
      } else {
        return 0;
      }
    });

  const harUtbetalingsType = sortedData.some((utbetaling) => utbetaling.type);

  return (
    <Table
      sort={sort}
      onSortChange={(sortKey) => handleSort(sortKey as ScopedSortState["orderBy"])}
      aria-label="Utbetalinger"
    >
      <Table.Header>
        <Table.Row>
          <TableColumnHeader sortKey="periodeStart" sortable>
            {utbetalingTekster.periode.start.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="periodeSlutt" sortable>
            {utbetalingTekster.periode.slutt.label}
          </TableColumnHeader>
          <Table.ColumnHeader sortKey="kostnadssteder" sortable>
            Kostnadssted
          </Table.ColumnHeader>
          <TableColumnHeader sortKey="belop" sortable align="right">
            {utbetalingTekster.beregning.belop.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="status" sortable align="right">
            Status
          </TableColumnHeader>
          <TableColumnHeader align="right" className="max-w-6" hidden={!harUtbetalingsType}>
            <HStack gap="2">
              Type
              <HelpText title="Hva betyr forkortelsene?">
                <VStack gap="1" className="text-left">
                  <div>
                    <b>KOR:</b> Korrigering - manuelt opprettet i Tiltaksadministrasjon
                  </div>
                  <div>
                    <b>INV:</b> Investering - manuelt opprettet utbetalingskrav fra Arrangør
                  </div>
                </VStack>
              </HelpText>
            </HStack>
          </TableColumnHeader>
          <TableColumnHeader></TableColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map(({ belopUtbetalt, periode, id, status, kostnadssteder, type }) => {
          return (
            <Table.Row key={id}>
              <Table.DataCell>{formaterPeriodeStart(periode)}</Table.DataCell>
              <Table.DataCell>{formaterPeriodeSlutt(periode)}</Table.DataCell>
              <Table.DataCell
                aria-label={`Kostnadssteder: ${kostnadssteder
                  .map((kostnadssted) => kostnadssted.navn)
                  .join(", ")}`}
                title={`Kostnadssteder: ${kostnadssteder
                  .map((kostnadssted) => kostnadssted.navn)
                  .join(", ")}`}
              >
                {formaterNavEnheter(
                  kostnadssteder.map((kostnadssted) => ({
                    navn: kostnadssted.navn,
                    enhetsnummer: kostnadssted.enhetsnummer,
                  })),
                )}
              </Table.DataCell>
              <Table.DataCell align="right">
                {belopUtbetalt ? formaterNOK(belopUtbetalt) : ""}
              </Table.DataCell>
              <Table.DataCell align="right">
                <UtbetalingStatusTag status={status} />
              </Table.DataCell>
              {harUtbetalingsType && (
                <Table.DataCell align="left">
                  {type && <UtbetalingTypeTag type={type} />}
                </Table.DataCell>
              )}
              <Table.DataCell>
                {status !== AdminUtbetalingStatus.VENTER_PA_ARRANGOR && (
                  <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger/${id}`}>
                    {[
                      AdminUtbetalingStatus.UTBETALT,
                      AdminUtbetalingStatus.OVERFORT_TIL_UTBETALING,
                    ].includes(status)
                      ? "Detaljer"
                      : "Behandle"}
                  </Link>
                )}
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
