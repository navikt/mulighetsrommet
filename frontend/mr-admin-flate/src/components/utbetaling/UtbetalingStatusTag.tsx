import { AdminUtbetalingStatus } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";
import { ReactNode } from "react";

export function UtbetalingStatusTag({ status }: { status: AdminUtbetalingStatus }): ReactNode {
  const baseTagClasses = "w-[150px] text-center whitespace-nowrap";

  switch (status) {
    case AdminUtbetalingStatus.RETURNERT:
      return (
        <Tag size="small" variant="error" className={baseTagClasses}>
          Returnert
        </Tag>
      );
    case AdminUtbetalingStatus.TIL_GODKJENNING:
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
          Til godkjenning
        </Tag>
      );
    case AdminUtbetalingStatus.GODKJENT:
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Godkjent
        </Tag>
      );
    case AdminUtbetalingStatus.UTBETALT:
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Utbetalt
        </Tag>
      );
    case AdminUtbetalingStatus.BEHANDLES_AV_NAV:
      return (
        <Tag size="small" variant="warning" className={baseTagClasses}>
          Behandles av Nav
        </Tag>
      );
    case AdminUtbetalingStatus.VENTER_PA_ARRANGOR:
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
          Venter på arrangør
        </Tag>
      );
  }
}
