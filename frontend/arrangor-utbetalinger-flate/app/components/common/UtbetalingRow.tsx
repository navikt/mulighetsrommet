import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { Table } from "@navikt/ds-react";
import { UtbetalingStatusTag } from "../utbetaling/UtbetalingStatusTag";
import { UtbetalingTextLink } from "../utbetaling/UtbetalingTextLink";
import {
  ArrangorflateFilterType,
  ArrangorflateUtbetalingRadDto,
} from "@arrangor-utbetalinger/api-client";
import { Kolonne } from "./Tabellvisning";
import { TiltakHeaderCell } from "~/components/common/TiltakHeaderCell";
import { ArrangorDataCell } from "~/components/common/ArrangorDataCell";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";

export function utbetalingKolonner(type: ArrangorflateFilterType): Array<Kolonne> {
  const kolonner: Array<Kolonne> = [
    { key: "tiltakNavn", label: "Tiltak", sortable: true },
    { key: "arrangorNavn", label: "Arrangør", sortable: true },
    { key: "startDato", label: "Periode", sortable: true },
    { key: "beregnetBelop", label: "Beløp", sortable: true },
  ];
  if (type === ArrangorflateFilterType.HISTORISKE) {
    kolonner.push({ key: "godkjentBelop", label: "Godkjent beløp", sortable: true });
  }
  kolonner.push({ key: "type", label: "Type" }, { key: "status", label: "Status", sortable: true });
  return kolonner;
}

interface UtbetalingRowProps {
  row: ArrangorflateUtbetalingRadDto;
  type: ArrangorflateFilterType;
}

export function UtbetalingRow({ row, type }: UtbetalingRowProps) {
  return (
    <Table.Row>
      <TiltakHeaderCell tiltakstype={row.tiltakstype} gjennomforing={row.gjennomforing} />

      <ArrangorDataCell arrangor={row.arrangor} />

      <Table.DataCell>{formaterPeriode(row.periode)}</Table.DataCell>

      <Table.DataCell align="right" className="whitespace-nowrap">
        {formaterValutaBelop(row.beregnetBelop)}
      </Table.DataCell>

      {type === ArrangorflateFilterType.HISTORISKE && (
        <Table.DataCell align="right" className="whitespace-nowrap">
          {row.godkjentBelop ? formaterValutaBelop(row.godkjentBelop) : null}
        </Table.DataCell>
      )}

      <Table.DataCell>
        <UtbetalingTypeTag type={row.type} />
      </Table.DataCell>

      <Table.DataCell>
        <UtbetalingStatusTag status={row.status} />
      </Table.DataCell>

      <Table.DataCell>
        <UtbetalingTextLink
          status={row.status}
          gjennomforingNavn={row.gjennomforing.navn}
          utbetalingId={row.utbetalingId}
          orgnr={row.arrangor.organisasjonsnummer}
        />
      </Table.DataCell>
    </Table.Row>
  );
}
