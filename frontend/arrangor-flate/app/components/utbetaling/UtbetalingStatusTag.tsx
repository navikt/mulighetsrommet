import { Tag, TagProps } from "@navikt/ds-react";
import { ArrFlateUtbetalingStatus } from "api-client";
import { ReactNode } from "react";

interface Props {
  status: ArrFlateUtbetalingStatus;
  size?: TagProps["size"];
}

export function UtbetalingStatusTag({ status, size }: Props): ReactNode {
  const tagSize = size || "medium";
  switch (status) {
    case ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING:
      return (
        <Tag variant="success" size={tagSize}>
          Overført til utbetaling
        </Tag>
      );
    case ArrFlateUtbetalingStatus.UTBETALT:
      return (
        <Tag variant="success" size={tagSize}>
          Utbetalt
        </Tag>
      );
    case ArrFlateUtbetalingStatus.BEHANDLES_AV_NAV:
      return (
        <Tag variant="warning" size={tagSize}>
          Behandles av Nav
        </Tag>
      );
    case ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING:
      return (
        <Tag variant="alt1" size={tagSize}>
          Klar for innsending
        </Tag>
      );
    case ArrFlateUtbetalingStatus.KREVER_ENDRING:
      return (
        <Tag variant="warning" size={tagSize}>
          Krever endring
        </Tag>
      );
  }
}
