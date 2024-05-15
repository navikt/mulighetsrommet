import classNames from "classnames";
import { HistorikkForBrukerStatus } from "mulighetsrommet-api-client";
import styles from "./Statusbadge.module.scss";
import { Detail } from "@navikt/ds-react";

export function StatusBadge({ status }: { status?: HistorikkForBrukerStatus }) {
  return (
    <Detail
      className={classNames(
        styles.historikk_for_bruker_statusbadge,
        styles[status as unknown as any],
      )}
    >
      {statustekst(status)}
    </Detail>
  );
}

function statustekst(status?: HistorikkForBrukerStatus): string {
  switch (status) {
    case "AVSLUTTET":
      return "Avsluttet";
    case "DELTAR":
      return "Deltar";
    case "IKKE_AKTUELL":
      return "Ikke aktuell";
    case "VENTER":
      return "Venter";
    default:
      return "";
  }
}
