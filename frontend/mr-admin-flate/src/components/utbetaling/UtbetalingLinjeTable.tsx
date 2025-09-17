import { UtbetalingDto, UtbetalingLinje } from "@tiltaksadministrasjon/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Box, CopyButton, HStack, Table } from "@navikt/ds-react";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import React from "react";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
  renderRow: (linje: UtbetalingLinje, index: number) => React.ReactNode;
}

function sum(numbers: number[]): number {
  return numbers.reduce((acc, d) => acc + (Number(d) || 0), 0);
}

export function UtbetalingLinjeTable({ linjer, utbetaling, renderRow }: Props) {
  const utbetalesTotal = sum(linjer.map((l) => l.belop));
  const totalGjenstaendeBelop = sum(linjer.map((l) => l.tilsagn.belopGjenstaende));
  const differanse = Number(utbetaling.belop) - utbetalesTotal;

  return (
    <Box className="overflow-x-scroll">
      <Table data-testid="linje-table">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell colSpan={6} className="bg-gray-100">
              Tilgjengelige tilsagn
            </Table.HeaderCell>
            <Table.HeaderCell colSpan={4}>Utbetalingdetaljer</Table.HeaderCell>
          </Table.Row>
          <Table.Row>
            <Table.HeaderCell className="bg-gray-100" />
            <Table.HeaderCell className="bg-gray-100">Tilsagnsnummer</Table.HeaderCell>
            <Table.HeaderCell scope="col" className="bg-gray-100">
              Type
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" className="bg-gray-100">
              Periode
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" className="bg-gray-100">
              Kostnadssted
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" className="bg-gray-100">
              Gjenstående beløp
            </Table.HeaderCell>
            <Table.HeaderCell scope="col">Gjør opp tilsagn</Table.HeaderCell>
            <Table.HeaderCell scope="col">
              {utbetalingTekster.beregning.utbetales.label}
            </Table.HeaderCell>
            <Table.HeaderCell scope="col">Status</Table.HeaderCell>
            <Table.HeaderCell scope="col" />
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {linjer.map((linje, i) => renderRow(linje, i))}
          <Table.Row shadeOnHover={false}>
            <Table.DataCell colSpan={5} className="font-bold">
              {`${utbetalingTekster.beregning.belop.label}: ${formaterNOK(utbetaling.belop)}`}
            </Table.DataCell>
            <Table.DataCell className="font-bold" colSpan={2}>
              {formaterNOK(totalGjenstaendeBelop)}
            </Table.DataCell>
            <Table.DataCell className="font-bold">{formaterNOK(utbetalesTotal)}</Table.DataCell>
            <Table.DataCell className="font-bold" align="right" colSpan={2}>
              <HStack align="center">
                <CopyButton
                  variant="action"
                  copyText={differanse.toString()}
                  size="small"
                  text={`Differanse ${formaterNOK(differanse)}`}
                />
              </HStack>
            </Table.DataCell>
          </Table.Row>
        </Table.Body>
      </Table>
    </Box>
  );
}
