import { Tag } from "@navikt/ds-react";
import { AkselColor } from "@navikt/ds-react/types/theme";
import { TilsagnStatus } from "api-client";
import { ReactNode } from "react";

const statusConfig: Record<TilsagnStatus, { label: string; color: AkselColor } | null> = {
  [TilsagnStatus.RETURNERT]: null,
  [TilsagnStatus.TIL_GODKJENNING]: null,
  [TilsagnStatus.GODKJENT]: {
    label: "Godkjent",
    color: "success",
  },
  [TilsagnStatus.TIL_ANNULLERING]: {
    label: "Til annullering",
    color: "warning",
  },
  [TilsagnStatus.ANNULLERT]: {
    label: "Annullert",
    color: "danger",
  },
  [TilsagnStatus.TIL_OPPGJOR]: {
    label: "Til oppgjør",
    color: "warning",
  },
  [TilsagnStatus.OPPGJORT]: {
    label: "Oppgjort",
    color: "neutral",
  },
};

export function TilsagnStatusTag({ status }: { status: TilsagnStatus }): ReactNode {
  const config = statusConfig[status];

  if (!config) {
    return null;
  }

  return (
    <Tag size="small" data-color={config.color} className="whitespace-nowrap">
      {config.label}
    </Tag>
  );
}
