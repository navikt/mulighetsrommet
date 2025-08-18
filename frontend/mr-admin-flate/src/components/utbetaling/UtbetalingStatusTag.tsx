import { UtbetalingStatusDto } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";
import { ReactNode } from "react";

export function UtbetalingStatusTag({ status }: { status: UtbetalingStatusDto }): ReactNode {
  const baseTagClasses = "w-[150px] text-center whitespace-nowrap";

  switch (status.type) {
    case "RETURNERT":
      return (
        <Tag size="small" variant="error" className={baseTagClasses}>
          Returnert
        </Tag>
      );
    case "TIL_ATTESTERING":
      return (
        <Tag size="small" variant="warning" className={baseTagClasses}>
          Til attestering
        </Tag>
      );
    case "KLAR_TIL_BEHANDLING":
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Klar til behandling
        </Tag>
      );
    case "VENTER_PA_ARRANGOR":
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
          Venter på arrangør
        </Tag>
      );
    case "OVERFORT_TIL_UTBETALING":
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Overført til utbetaling
        </Tag>
      );
  }
}
