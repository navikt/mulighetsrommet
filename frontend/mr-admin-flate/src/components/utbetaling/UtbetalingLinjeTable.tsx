import { UtbetalingDto, UtbetalingLinje } from "@tiltaksadministrasjon/api-client";
import { formaterValuta, formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Box, CopyButton, HStack, Table } from "@navikt/ds-react";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import React from "react";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
  renderRow: (linje: UtbetalingLinje, index: number) => React.ReactNode;
}

export function UtbetalingLinjeTable({ linjer, utbetaling, renderRow }: Props) {
  const utbetalesTotal = linjer.reduce((acc, d) => acc + d.pris.belop, 0);
  const totalGjenstaendeBelop = linjer.reduce(
    (acc, l) => acc + l.tilsagn.belopGjenstaende.belop,
    0,
  );
  const differanse = utbetaling.pris.belop - utbetalesTotal;

  return (
    <Box className="overflow-x-scroll">
      <Table data-testid="linje-table">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell colSpan={6} className="bg-ax-neutral-200">
              Tilgjengelige tilsagn
            </Table.HeaderCell>
            <Table.HeaderCell colSpan={4}>Utbetalingdetaljer</Table.HeaderCell>
          </Table.Row>
          <Table.Row>
            <Table.HeaderCell className="bg-ax-neutral-200" />
            <Table.HeaderCell className="bg-ax-neutral-200">Tilsagnsnummer</Table.HeaderCell>
            <Table.HeaderCell scope="col" className="bg-ax-neutral-200">
              Type
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" className="bg-ax-neutral-200">
              Periode
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" className="bg-ax-neutral-200">
              Kostnadssted
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" className="bg-ax-neutral-200">
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
            <Table.DataCell colSpan={5} className="font-ax-bold">
              {`${utbetalingTekster.beregning.belop.label}: ${formaterValutaBelop(utbetaling.pris)}`}
            </Table.DataCell>
            <Table.DataCell className="font-ax-bold" colSpan={2}>
              {formaterValuta(totalGjenstaendeBelop, utbetaling.pris.valuta)}
            </Table.DataCell>
            <Table.DataCell className="font-ax-bold">
              {formaterValuta(utbetalesTotal, utbetaling.pris.valuta)}
            </Table.DataCell>
            <Table.DataCell className="font-ax-bold" align="right" colSpan={2}>
              <HStack align="center">
                <CopyButton
                  copyText={differanse.toString()}
                  size="small"
                  text={`Differanse ${formaterValuta(differanse, utbetaling.pris.valuta)}`}
                />
              </HStack>
            </Table.DataCell>
          </Table.Row>
        </Table.Body>
      </Table>
    </Box>
  );
}
