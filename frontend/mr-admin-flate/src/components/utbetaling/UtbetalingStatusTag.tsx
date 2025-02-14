import { UtbetalingStatus } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";
import { ReactNode } from "react";

export function UtbetalingStatusTag({ status }: { status: UtbetalingStatus }): ReactNode {
  const baseTagClasses = "w-[150px] text-center whitespace-nowrap";

  switch (status) {
    case UtbetalingStatus.UTBETALT:
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Utbetalt
        </Tag>
      );
    case UtbetalingStatus.INNSENDT_AV_NAV:
    case UtbetalingStatus.INNSENDT_AV_ARRANGOR:
      return (
        <Tag size="small" variant="warning" className={baseTagClasses}>
          Behandles av Nav
        </Tag>
      );
    case UtbetalingStatus.KLAR_FOR_GODKJENNING:
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
          Venter på arrangør
        </Tag>
      );
  }
}
