import { Tag } from "@navikt/ds-react";

interface Props {
  type:
    | "DELUTBETALING_UTBETALT"
    | "DELUTBETALING_OVERFORT_TIL_UTBETALING"
    | "DELUTBETALING_TIL_GODKJENNING"
    | "DELUTBETALING_AVVIST";
}

export function DelutbetalingTag({ type }: Props) {
  const baseTagClasses = "min-w-[140px] text-center whitespace-nowrap";

  switch (type) {
    case "DELUTBETALING_UTBETALT":
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Utbetalt
        </Tag>
      );
    case "DELUTBETALING_OVERFORT_TIL_UTBETALING":
      return (
        <Tag size="small" variant="warning" className={baseTagClasses}>
          Overført til utbetaling
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
