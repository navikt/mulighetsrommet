import { Tag, TagProps } from "@navikt/ds-react";
import { ArrangorflateUtbetalingStatus } from "api-client";
import { ReactNode } from "react";

interface Props {
  status: ArrangorflateUtbetalingStatus;
  size?: TagProps["size"];
}

export function UtbetalingStatusTag({ status, size }: Props): ReactNode {
  const tagSize = size || "medium";
  switch (status) {
    case ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING:
      return (
        <Tag variant="success" size={tagSize}>
          Overført til utbetaling
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.UTBETALT:
      return (
        <Tag variant="success" size={tagSize}>
          Utbetalt
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV:
      return (
        <Tag variant="warning" size={tagSize}>
          Behandles av Nav
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING:
      return (
        <Tag variant="alt1" size={tagSize}>
          Klar for innsending
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.KREVER_ENDRING:
      return (
        <Tag variant="warning" size={tagSize}>
          Krever endring
        </Tag>
      );
  }
}
