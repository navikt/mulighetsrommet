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
        <Tag size="small" variant="warning" className={baseTagClasses}>
          Til godkjenning
        </Tag>
      );
    case AdminUtbetalingStatus.UTBETALT:
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Utbetalt
        </Tag>
      );
    case AdminUtbetalingStatus.KLAR_TIL_BEHANDLING:
      return (
        <Tag size="small" variant="warning" className={baseTagClasses}>
          Klar til behandling
        </Tag>
      );
    case AdminUtbetalingStatus.VENTER_PA_ARRANGOR:
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
          Venter på arrangør
        </Tag>
      );
    case AdminUtbetalingStatus.OVERFORT_TIL_UTBETALING:
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Overført til utbetaling
        </Tag>
      );
  }
}
