import { Tag } from "@navikt/ds-react";
import { DelutbetalingStatus } from "api-client";
import { ReactNode } from "react";

interface Props {
  status: DelutbetalingStatus;
}

export function DelUtbetalingStatusTag({ status }: Props): ReactNode {
  switch (status) {
    case DelutbetalingStatus.OVERFORT_TIL_UTBETALING:
      return (
        <Tag data-color="success" variant="outline" size="small">
          Overført til utbetaling
        </Tag>
      );
    case DelutbetalingStatus.UTBETALT:
      return (
        <Tag data-color="success" variant="outline" size="small">
          Utbetalt
        </Tag>
      );
    case DelutbetalingStatus.TIL_ATTESTERING:
    case DelutbetalingStatus.GODKJENT:
    case DelutbetalingStatus.RETURNERT:
      return (
        <Tag data-color="info" variant="outline" size="small">
          Behandles av Nav
        </Tag>
      );
  }
}
