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
          <Table.HeaderCell colSpan={7} className="bg-gray-100">
            Tilgjengelige tilsagn
          </Table.HeaderCell>
          <Table.HeaderCell colSpan={6}>Utbetalingdetaljer</Table.HeaderCell>
        </Table.Row>
        <Table.Row>
          <Table.HeaderCell className="bg-gray-100" />
          <Table.HeaderCell className="bg-gray-100">Tilsagnsnummer</Table.HeaderCell>
          <Table.HeaderCell scope="col" className="bg-gray-100">
            Type
          </Table.HeaderCell>
          <Table.HeaderCell scope="col" className="bg-gray-100">
            Periodestart
          </Table.HeaderCell>
          <Table.HeaderCell scope="col" className="bg-gray-100">
            Periodeslutt
          </Table.HeaderCell>
          <Table.HeaderCell scope="col" className="bg-gray-100">
            Kostnadssted
          </Table.HeaderCell>
          <Table.HeaderCell scope="col" className="bg-gray-100">
            Tilgjengelig
          </Table.HeaderCell>
          <Table.HeaderCell scope="col">Gjør opp tilsagn</Table.HeaderCell>
          <Table.HeaderCell scope="col">Utbetales</Table.HeaderCell>
          <Table.HeaderCell scope="col" />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {linjer.map((linje, i) => renderRow(linje, i))}
        <Table.Row shadeOnHover={false}>
          <Table.DataCell
            className="font-bold"
            colSpan={6}
          >{`${utbetalingTekster.beregning.belop.label}: ${formaterNOK(utbetaling.beregning.belop)}`}</Table.DataCell>
          <Table.DataCell colSpan={2} className="font-bold">
            {formaterNOK(totalGjenstaendeBelop)}
          </Table.DataCell>
          <Table.DataCell className="font-bold">{formaterNOK(utbetalesTotal)}</Table.DataCell>
          <Table.DataCell colSpan={4} className="font-bold">
            <HStack align="center">
              <CopyButton variant="action" copyText={differanse.toString()} size="small" />
              <BodyShort weight="semibold">{`Differanse ${formaterNOK(differanse)}`}</BodyShort>
            </HStack>
          </Table.DataCell>
        </Table.Row>
      </Table.Body>
    </Table>
  );
}
