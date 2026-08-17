import { Tag } from "@navikt/ds-react";
import { AkselColor } from "@navikt/ds-react/types/theme";
import { ArrangorflateUtbetalingStatus } from "@arrangor-utbetalinger/api-client";
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
  [ArrangorflateUtbetalingStatus.BLOKKERT_FOR_INNSENDING]: {
    label: "Blokkert for innsending",
    color: "warning",
  },
  [ArrangorflateUtbetalingStatus.AVBRUTT_AV_ARRANGOR]: {
    label: "Avbrutt av arrangør",
    color: "neutral",
  },
  [ArrangorflateUtbetalingStatus.AVBRUTT_AV_NAV]: {
    label: "Avbrutt av Nav",
    color: "neutral",
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
