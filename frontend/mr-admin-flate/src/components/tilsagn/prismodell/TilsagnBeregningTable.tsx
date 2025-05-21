import { TilsagnBeregningFriInputLinje } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Table } from "@navikt/ds-react";
import { tilsagnTekster } from "../TilsagnTekster";

interface TilsagnBeregningTableProps {
  linjer: TilsagnBeregningFriInputLinje[];
  medRadnummer?: boolean;
}
export function TilsagnBeregningTable({ linjer, medRadnummer }: TilsagnBeregningTableProps) {
  if (!linjer.length) {
    return null;
  }
  return (
    <Table size="small">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">
            {medRadnummer
              ? tilsagnTekster.beregning.input.linjer.rad.label
              : tilsagnTekster.beregning.input.linjer.beskrivelse.label}
          </Table.HeaderCell>
          <Table.HeaderCell scope="col" align="right">
            {tilsagnTekster.beregning.input.linjer.belop.label}
          </Table.HeaderCell>
          <Table.HeaderCell scope="col" align="right">
            {tilsagnTekster.beregning.input.linjer.antall.label}
          </Table.HeaderCell>
          <Table.HeaderCell scope="col" align="right">
            {tilsagnTekster.beregning.input.linjer.delsum.label}
          </Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {linjer.map(({ id, beskrivelse, belop, antall }: any, i: number) => {
          return (
            <Table.Row key={id}>
              {medRadnummer ? (
                <Table.HeaderCell scope="row">{i + 1}</Table.HeaderCell>
              ) : (
                <Table.DataCell>{beskrivelse}</Table.DataCell>
              )}

              <Table.DataCell align="right">{formaterNOK(belop)}</Table.DataCell>
              <Table.DataCell align="right">{antall}</Table.DataCell>
              <Table.DataCell align="right">{formaterNOK(belop * antall)}</Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
