import { Tag } from "@navikt/ds-react";
import { AkselColor } from "@navikt/ds-react/types/theme";
import { ArrangorflateUtbetalingStatus } from "api-client";
import { ReactNode } from "react";

interface Props {
  status: ArrangorflateUtbetalingStatus;
}

const utbetalingStatusConfig: Record<
  ArrangorflateUtbetalingStatus,
  { label: string; color: AkselColor }
> = {
  [ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING]: {
    label: "Overført til utbetaling",
    color: "success",
  },
  [ArrangorflateUtbetalingStatus.UTBETALT]: {
    label: "Utbetalt",
    color: "success",
  },
  [ArrangorflateUtbetalingStatus.DELVIS_UTBETALT]: {
    label: "Delvis utbetalt",
    color: "success",
  },
  [ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV]: {
    label: "Behandles av Nav",
    color: "warning",
  },
  [ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING]: {
    label: "Klar for innsending",
    color: "meta-purple",
  },
  [ArrangorflateUtbetalingStatus.UBEHANDLET_FORSLAG]: {
    label: "Ubehandlede forslag",
    color: "warning",
  },
  [ArrangorflateUtbetalingStatus.AVBRUTT]: {
    label: "Avbrutt av arrangør",
    color: "danger",
  },
};

export function UtbetalingStatusTag({ status }: Props): ReactNode {
  const config = utbetalingStatusConfig[status];
  return (
    <Tag data-color={config.color} size="small" className="whitespace-nowrap">
      {config.label}
    </Tag>
  );
}
