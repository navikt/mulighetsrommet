import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { formaterPeriodeUdefinertSlutt } from "@mr/frontend-common/utils/date";
import { Table } from "@navikt/ds-react";
import { UtbetalingStatusTag } from "../utbetaling/UtbetalingStatusTag";
import { UtbetalingTextLink } from "../utbetaling/UtbetalingTextLink";
import { ArrangorflateUtbetalingRadDto } from "@arrangor-utbetalinger/api-client";
import { Kolonne } from "./Tabellvisning";
import { TiltakHeaderCell } from "~/components/common/TiltakHeaderCell";
import { ArrangorDataCell } from "~/components/common/ArrangorDataCell";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";

export const utbetalingKolonner: Array<Kolonne> = [
  { key: "tiltakNavn", label: "Tiltak", sortable: true },
  { key: "arrangorNavn", label: "Arrangør", sortable: true },
  { key: "startDato", label: "Periode", sortable: true },
  { key: "belop", label: "Beløp", sortable: true },
  { key: "type", label: "Type" },
  { key: "status", label: "Status", sortable: true },
];
export function UtbetalingRow({ row }: { row: ArrangorflateUtbetalingRadDto }) {
  return (
    <Table.Row>
      <TiltakHeaderCell
        tiltakstype={row.tiltakstypeNavn}
        navn={row.tiltakNavn}
        lopenummer={row.lopenummer}
      />

      <ArrangorDataCell navn={row.arrangorNavn} organisasjonsnummer={row.organisasjonsnummer} />

      <Table.DataCell>
        {formaterPeriodeUdefinertSlutt({ start: row.startDato, slutt: row.sluttDato })}
      </Table.DataCell>

      <Table.DataCell align="right" className="whitespace-nowrap">
        {formaterValutaBelop(row.belop)}
      </Table.DataCell>

      <Table.DataCell>
        <UtbetalingTypeTag type={row.type} />
      </Table.DataCell>

      <Table.DataCell>
        <UtbetalingStatusTag status={row.status} />
      </Table.DataCell>

      <Table.DataCell>
        <UtbetalingTextLink
          status={row.status}
          gjennomforingNavn={row.tiltakNavn}
          utbetalingId={row.utbetalingId}
          orgnr={row.organisasjonsnummer}
        />
      </Table.DataCell>
    </Table.Row>
  );
}
