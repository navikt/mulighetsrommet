import { UtbetalingStatus } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";
import { ReactNode } from "react";

export function UtbetalingStatusTag({ status }: { status: UtbetalingStatus }): ReactNode {
  const baseTagClasses = "w-[140px] text-center whitespace-nowrap";

  switch (status) {
    case UtbetalingStatus.GODKJENT_AV_ARRANGOR:
      return (
        <Tag size="small" variant="neutral" className={baseTagClasses}>
          Godkjent
        </Tag>
      );
    case UtbetalingStatus.KLAR_FOR_GODKJENNING:
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
          Klar for innsending
        </Tag>
      );
  }
}
