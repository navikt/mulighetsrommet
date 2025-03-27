import { UtbetalingDto, UtbetalingLinje } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BodyShort, CopyButton, HStack, Table } from "@navikt/ds-react";
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
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell />
          <Table.HeaderCell scope="col">Periodestart</Table.HeaderCell>
          <Table.HeaderCell scope="col">Periodeslutt</Table.HeaderCell>
          <Table.HeaderCell scope="col">Type</Table.HeaderCell>
          <Table.HeaderCell scope="col">Kostnadssted</Table.HeaderCell>
          <Table.HeaderCell scope="col">Tilgjengelig på tilsagn</Table.HeaderCell>
          <Table.HeaderCell scope="col">Gjør opp tilsagn</Table.HeaderCell>
          <Table.HeaderCell scope="col">Utbetales</Table.HeaderCell>
          <Table.HeaderCell scope="col">Status</Table.HeaderCell>
          <Table.HeaderCell scope="col" />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {linjer.map((linje, i) => renderRow(linje, i))}
        <Table.Row>
          <Table.DataCell
            className="font-bold"
            colSpan={5}
          >{`${utbetalingTekster.beregning.belop.label}: ${formaterNOK(utbetaling.beregning.belop)}`}</Table.DataCell>
          <Table.DataCell colSpan={2} className="font-bold">
            {formaterNOK(totalGjenstaendeBelop)}
          </Table.DataCell>
          <Table.DataCell className="font-bold">{formaterNOK(utbetalesTotal)}</Table.DataCell>
          <Table.DataCell colSpan={2} className="font-bold">
            <HStack align="center" className="w-80">
              <CopyButton variant="action" copyText={differanse.toString()} size="small" />
              <BodyShort weight="semibold">{`Differanse ${formaterNOK(differanse)}`}</BodyShort>
            </HStack>
          </Table.DataCell>
        </Table.Row>
      </Table.Body>
    </Table>
  );
}
