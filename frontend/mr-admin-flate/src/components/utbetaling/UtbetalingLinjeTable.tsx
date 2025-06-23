import { UtbetalingDto, UtbetalingLinje } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Box, CopyButton, HStack, Table } from "@navikt/ds-react";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
  renderRow: (linje: UtbetalingLinje, index: number) => React.ReactNode;
}

export function UtbetalingLinjeTable({ linjer, utbetaling, renderRow }: Props) {
  const utbetalesTotal = linjer.reduce((acc, d) => acc + d.belop, 0);
  const totalGjenstaendeBelop = linjer.reduce((acc, l) => acc + l.tilsagn.belopGjenstaende, 0);
  const differanse = utbetaling.beregning.belop - utbetalesTotal;

  return (
    <Box className="overflow-x-scroll">
      <Table>
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
              {`${utbetalingTekster.beregning.belop.label}: ${formaterNOK(utbetaling.beregning.belop)}`}
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
