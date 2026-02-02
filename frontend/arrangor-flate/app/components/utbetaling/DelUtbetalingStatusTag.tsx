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
        <Tag data-color="success" variant="outline" size={tagSize}>
          Overført til utbetaling
        </Tag>
      );
    case DelutbetalingStatus.UTBETALT:
      return (
        <Tag data-color="success" variant="outline" size={tagSize}>
          Utbetalt
        </Tag>
      );
    case DelutbetalingStatus.TIL_ATTESTERING:
    case DelutbetalingStatus.GODKJENT:
    case DelutbetalingStatus.RETURNERT:
      return (
        <Tag data-color="info" variant="outline" size={tagSize}>
          Behandles av Nav
        </Tag>
      );
  }
}
