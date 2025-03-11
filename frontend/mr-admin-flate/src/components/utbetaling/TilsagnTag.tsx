import { TilsagnStatus } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";

interface Props {
  status: TilsagnStatus;
}

export function TilsagnStatusTag({ status }: Props) {
  const baseTagClasses = "min-w-[140px] text-center whitespace-nowrap";

  switch (status) {
    case TilsagnStatus.TIL_GODKJENNING:
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
          Tilsagn til godkjenning
        </Tag>
      );
    case TilsagnStatus.RETURNERT:
      return (
        <Tag size="small" variant="error" className={baseTagClasses}>
          Tilsagn returnert
        </Tag>
      );
    case TilsagnStatus.TIL_ANNULLERING:
      return (
        <Tag size="small" variant="error" className={baseTagClasses}>
          Tilsagn til annullering
        </Tag>
      );
    case TilsagnStatus.ANNULLERT:
      return (
        <Tag size="small" variant="error" className={baseTagClasses}>
          Tilsagn annulert
        </Tag>
      );
  }
}
