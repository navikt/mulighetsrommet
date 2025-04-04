import { DelutbetalingStatus } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";

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
        <Tag size="small" variant="warning" className={baseTagClasses}>
          Til godkjenning
        </Tag>
      );
    case DelutbetalingStatus.RETURNERT:
      return (
        <Tag size="small" variant="error" className={baseTagClasses}>
          Returnert
        </Tag>
      );
    case DelutbetalingStatus.OVERFORT_TIL_UTBETALING:
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Overført til utbetaling
        </Tag>
      );
  }
}
