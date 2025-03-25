import { UtbetalingDto, UtbetalingLinje } from "@mr/api-client-v2";
import { DelutbetalingRow } from "./DelutbetalingRow";
import { BodyShort, CopyButton, Heading, HStack, Table } from "@navikt/ds-react";
import { utbetalingTekster } from "./UtbetalingTekster";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
}

export function UtbetalingLinjeRows({ linjer, utbetaling }: Props) {
  const utbetalesTotal = linjer.reduce((acc, d) => acc + d.belop, 0);
  const totalGjenstaendeBelop = linjer.reduce((acc, l) => acc + l.tilsagn.belopGjenstaende, 0);
  const differanse = utbetaling.beregning.belop - utbetalesTotal;

  return (
    <>
      <Heading size="medium">Utbetalingslinjer</Heading>
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
          {linjer
            .toSorted((m, n) => m.id.localeCompare(n.id))
            .map((linje) => (
              <DelutbetalingRow linje={linje} key={linje.id} />
            ))}
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
    </>
  );
}
