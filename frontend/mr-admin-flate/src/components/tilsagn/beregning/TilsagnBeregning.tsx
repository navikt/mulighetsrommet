import {
  TilsagnBeregningDto,
  TilsagnBeregningFastSatsPerTiltaksplassPerManed,
  TilsagnBeregningFriInputLinje,
  TilsagnBeregningPrisPerManedsverk,
  TilsagnBeregningPrisPerUkesverk,
} from "@mr/api-client-v2";
import { BodyShort, HStack, Table } from "@navikt/ds-react";
import { tilsagnTekster } from "../TilsagnTekster";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

interface Props {
  beregning: TilsagnBeregningDto;
  redigeringsModus?: boolean;
}

export function TilsagnBeregning({ beregning, redigeringsModus }: Props) {
  switch (beregning.type) {
    case "FRI":
      return (
        <FriBeregningTable
          medRadnummer={redigeringsModus}
          medBeskrivelse={!redigeringsModus}
          linjer={beregning.linjer}
        />
      );
    case "PRIS_PER_UKESVERK":
      return <PrisPerUkesverkBeregning beregning={beregning} />;
    case "PRIS_PER_MANEDSVERK":
    case "FAST_SATS_PER_TILTAKSPLASS_PER_MANED":
      return <PrisPerManedsverkBeregning beregning={beregning} />;
  }
}

interface TilsagnBeregningTableProps {
  linjer: TilsagnBeregningFriInputLinje[];
  medRadnummer?: boolean;
  medBeskrivelse?: boolean;
}

function FriBeregningTable({ linjer, medRadnummer, medBeskrivelse }: TilsagnBeregningTableProps) {
  if (!linjer.length) {
    return null;
  }
  return (
    <Table size="small">
      <Table.Header>
        <Table.Row>
          {medRadnummer && (
            <Table.HeaderCell scope="col">
              {tilsagnTekster.beregning.input.linjer.rad.label}
            </Table.HeaderCell>
          )}
          {medBeskrivelse && (
            <Table.HeaderCell scope="col">
              {tilsagnTekster.beregning.input.linjer.beskrivelse.label}
            </Table.HeaderCell>
          )}
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
              {medRadnummer && <Table.HeaderCell scope="row">{i + 1}</Table.HeaderCell>}
              {medBeskrivelse && <Table.DataCell>{beskrivelse}</Table.DataCell>}
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

function PrisPerUkesverkBeregning({ beregning }: { beregning: TilsagnBeregningPrisPerUkesverk }) {
  return (
    <HStack align="center" gap="2">
      <BodyShort>
        {beregning.antallPlasser} plasser × {beregning.sats} × {beregning.antallUker} uker ={" "}
        {formaterNOK(beregning.belop)}
      </BodyShort>
    </HStack>
  );
}

function PrisPerManedsverkBeregning({
  beregning,
}: {
  beregning: TilsagnBeregningPrisPerManedsverk | TilsagnBeregningFastSatsPerTiltaksplassPerManed;
}) {
  return (
    <HStack align="center" gap="2">
      <BodyShort>
        {beregning.antallPlasser} plasser × {formaterNOK(beregning.sats)} ×{" "}
        {beregning.antallManeder} måneder = {formaterNOK(beregning.belop)}
      </BodyShort>
    </HStack>
  );
}
