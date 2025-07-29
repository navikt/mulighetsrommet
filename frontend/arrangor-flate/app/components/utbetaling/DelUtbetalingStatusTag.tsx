import { Tag, TagProps } from "@navikt/ds-react";
import { DelutbetalingStatus } from "api-client";
import { ReactNode } from "react";

interface Props {
  status: DelutbetalingStatus;
  size?: TagProps["size"];
}

export function DelUtbetalingStatusTag({ status, size }: Props): ReactNode {
  const tagSize = size || "medium";
  switch (status) {
    case DelutbetalingStatus.OVERFORT_TIL_UTBETALING:
      return (
        <Tag variant="success" size={tagSize}>
          Overført til utbetaling
        </Tag>
      );
    case DelutbetalingStatus.UTBETALT:
      return (
        <Tag variant="success" size={tagSize}>
          Utbetalt
        </Tag>
      );
    case DelutbetalingStatus.TIL_ATTESTERING:
    case DelutbetalingStatus.GODKJENT:
    case DelutbetalingStatus.RETURNERT:
      return (
        <Tag variant="info" size={tagSize}>
          Behandles av Nav
        </Tag>
      );
  }
}
