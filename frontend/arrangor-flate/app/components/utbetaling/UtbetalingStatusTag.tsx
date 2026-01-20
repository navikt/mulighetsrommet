import { Tag, TagProps } from "@navikt/ds-react";
import { ArrangorflateUtbetalingStatus } from "api-client";
import { ReactNode } from "react";

interface Props {
  status: ArrangorflateUtbetalingStatus;
  size?: TagProps["size"];
}

export function UtbetalingStatusTag({ status }: Props): ReactNode {
  switch (status) {
    case ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING:
      return (
        <Tag variant="success" size="small">
          Overført til utbetaling
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.UTBETALT:
      return (
        <Tag variant="success" size="small">
          Utbetalt
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.DELVIS_UTBETALT:
      return (
        <Tag variant="success" size="small">
          Delvis utbetalt
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV:
      return (
        <Tag variant="warning" size="small">
          Behandles av Nav
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING:
      return (
        <Tag variant="alt1" size="small">
          Klar for innsending
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.KREVER_ENDRING:
      return (
        <Tag variant="warning" size="small">
          Krever endring
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.AVBRUTT:
      return (
        <Tag variant="error" size="small">
          Avbrutt av arrangør
        </Tag>
      );
  }
}
