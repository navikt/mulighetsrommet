import { formaterNavEnheter } from "@/utils/Utils";
import { NavEnhetDto } from "@mr/api-client-v2";
import { UtbetalingKompaktDto, UtbetalingStatusDto } from "@tiltaksadministrasjon/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { HelpText, HStack, Table, VStack } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { useMemo } from "react";
import { Link, useParams } from "react-router";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { useSortableData } from "@mr/frontend-common";
import { formaterPeriodeSlutt, formaterPeriodeStart } from "@mr/frontend-common/utils/date";

interface Props {
  utbetalinger: UtbetalingKompaktDto[];
}

interface UtbetalingRow {
  periodeStart: string;
  periodeSlutt: string;
  status: UtbetalingStatusDto;
  kostnadssteder: NavEnhetDto[];
}

export function UtbetalingTable({ utbetalinger }: Props) {
  const { gjennomforingId } = useParams();
  const { sortedData, sort, toggleSort } = useSortableData(
    useMemo(() => {
      return utbetalinger.map((u) => ({
        ...u,
        periodeStart: u.periode.start,
        periodeSlutt: u.periode.slutt,
      }));
    }, [utbetalinger]),
  );

  const harUtbetalingsTypeTag = sortedData.some((utbetaling) => utbetaling.type.tagName);

  return (
    <Table
      sort={sort}
      onSortChange={(sortKey) => toggleSort(sortKey as keyof UtbetalingRow)}
      aria-label="Utbetalinger"
      data-testid="utbetaling-table"
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
          <TableColumnHeader sortKey="belopUtbetalt" sortable align="right">
            {utbetalingTekster.beregning.belop.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="status" sortable align="right">
            Status
          </TableColumnHeader>
          <TableColumnHeader align="right" className="max-w-6" hidden={!harUtbetalingsTypeTag}>
            <HStack gap="2">
              Type
              <HelpText title="Hva betyr forkortelsene?">
                <VStack gap="1" className="text-left">
                  <div>
                    <b>KOR:</b> Korreksjon på utbetaling
                  </div>
                  <div>
                    <b>INV:</b> Utbetaling for investering
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
              {harUtbetalingsTypeTag && (
                <Table.DataCell align="left">
                  <UtbetalingTypeTag type={type} />
                </Table.DataCell>
              )}
              <Table.DataCell>
                <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger/${id}`}>
                  {["OVERFORT_TIL_UTBETALING", "AVBRUTT", "VENTER_PA_ARRANGOR"].includes(
                    status.type,
                  )
                    ? "Detaljer"
                    : "Behandle"}
                </Link>
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
