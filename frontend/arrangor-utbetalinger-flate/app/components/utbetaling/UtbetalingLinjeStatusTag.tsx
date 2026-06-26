import { Tag } from "@navikt/ds-react";
import { UtbetalingLinjeStatus } from "@arrangor-utbetalinger/api-client";
import { ReactNode } from "react";

interface Props {
  status: UtbetalingLinjeStatus;
}

export function UtbetalingLinjeStatusTag({ status }: Props): ReactNode {
  switch (status) {
    case UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING:
      return (
        <Tag data-color="success" variant="outline" size="small">
          Overført til utbetaling
        </Tag>
      );
    case UtbetalingLinjeStatus.UTBETALT:
      return (
        <Tag data-color="success" variant="outline" size="small">
          Utbetalt
        </Tag>
      );
    case UtbetalingLinjeStatus.TIL_ATTESTERING:
    case UtbetalingLinjeStatus.GODKJENT:
    case UtbetalingLinjeStatus.RETURNERT:
      return (
        <Tag data-color="info" variant="outline" size="small">
          Behandles av Nav
        </Tag>
      );
    case UtbetalingLinjeStatus.AVBRUTT:
      return (
        <Tag data-color="danger" variant="outline" size="small">
          Avbrutt
        </Tag>
      );
  }
}
