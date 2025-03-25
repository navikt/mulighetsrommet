import { Tag } from "@navikt/ds-react";
import { DelutbetalingStatus } from "@mr/api-client-v2";

interface Props {
  status: DelutbetalingStatus;
}

export function DelutbetalingTag({ status }: Props) {
  const baseTagClasses = "min-w-[140px] text-center whitespace-nowrap";

  switch (status) {
    case DelutbetalingStatus.UTBETALT:
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Utbetalt
        </Tag>
      );
    case DelutbetalingStatus.GODKJENT:
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Godkjent
        </Tag>
      );
    case DelutbetalingStatus.TIL_GODKJENNING:
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
          Til godkjenning
        </Tag>
      );
    case DelutbetalingStatus.RETURNERT:
      return (
        <Tag size="small" variant="error" className={baseTagClasses}>
          Returnert
        </Tag>
      );
  }
}
