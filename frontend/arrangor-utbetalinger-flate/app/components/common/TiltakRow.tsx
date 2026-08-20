import {
  addDuration,
  formaterPeriodeUdefinertSlutt,
  yyyyMMddFormatting,
} from "@mr/frontend-common/utils/date";
import { Link, Table } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { ArrangorflateTiltakRadDto } from "@arrangor-utbetalinger/api-client";
import { pathTo } from "~/utils/navigation";
import { Kolonne } from "./Tabellvisning";
import { TiltakHeaderCell } from "~/components/common/TiltakHeaderCell";
import { ArrangorDataCell } from "~/components/common/ArrangorDataCell";

export const kolonner: Array<Kolonne> = [
  { key: "tiltakNavn", label: "Tiltak", sortable: true },
  { key: "arrangorNavn", label: "Arrangør", sortable: true },
  { key: "startDato", label: "Periode", sortable: true },
];

export function TiltakRow({ row }: { row: ArrangorflateTiltakRadDto }) {
  // Gjennomføringens sluttdato er inklusiv, mens formaterPeriodeUdefinertSlutt forventer eksklusiv sluttdato
  const eksklusivSluttDato = row.sluttDato
    ? yyyyMMddFormatting(addDuration(row.sluttDato, { days: 1 }))
    : undefined;

  return (
    <Table.Row>
      <TiltakHeaderCell tiltakstype={row.tiltakstype} gjennomforing={row.gjennomforing} />

      <ArrangorDataCell arrangor={row.arrangor} />

      <Table.DataCell>
        {formaterPeriodeUdefinertSlutt({ start: row.startDato, slutt: eksklusivSluttDato })}
      </Table.DataCell>

      <Table.DataCell>
        <Link
          as={ReactRouterLink}
          aria-label={`Start innsending for krav om utbetaling for ${row.gjennomforing.navn}`}
          to={pathTo.opprettKrav(row.arrangor.organisasjonsnummer, row.gjennomforing.id)}
          className="whitespace-nowrap"
        >
          Start innsending
        </Link>
      </Table.DataCell>
    </Table.Row>
  );
}
