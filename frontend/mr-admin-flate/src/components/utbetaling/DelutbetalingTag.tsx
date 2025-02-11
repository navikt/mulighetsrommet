import { DelutbetalingDto } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";

interface Props {
  delutbetaling: DelutbetalingDto;
}

export function DelutbetalingTag({ delutbetaling }: Props) {
  const baseTagClasses = "min-w-[140px] text-center whitespace-nowrap";

  switch (delutbetaling.type) {
    case "DELUTBETALING_GODKJENT":
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Godkjent
        </Tag>
      );
    case "DELUTBETALING_TIL_GODKJENNING":
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
          Til godkjenning
        </Tag>
      );
    case "DELUTBETALING_AVVIST":
      return (
        <Tag size="small" variant="error" className={baseTagClasses}>
          Returnert
        </Tag>
      );
  }
}
