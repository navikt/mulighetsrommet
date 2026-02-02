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
        <Tag data-color="success" variant="outline" size="small">
          Overført til utbetaling
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.UTBETALT:
      return (
        <Tag data-color="success" variant="outline" size="small">
          Utbetalt
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.DELVIS_UTBETALT:
      return (
        <Tag data-color="success" variant="outline" size="small">
          Delvis utbetalt
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV:
      return (
        <Tag data-color="warning" variant="outline" size="small">
          Behandles av Nav
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING:
      return (
        <Tag data-color="meta-purple" variant="outline" size="small">
          Klar for innsending
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.KREVER_ENDRING:
      return (
        <Tag data-color="warning" variant="outline" size="small">
          Krever endring
        </Tag>
      );
    case ArrangorflateUtbetalingStatus.AVBRUTT:
      return (
        <Tag data-color="danger" variant="outline" size="small">
          Avbrutt av arrangør
        </Tag>
      );
  }
}
