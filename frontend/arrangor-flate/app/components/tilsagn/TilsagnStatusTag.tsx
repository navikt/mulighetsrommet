import { DataElementStatus, DataElementStatusVariant, TilsagnStatus } from "api-client";

export function tilsagnStatusElement(status: TilsagnStatus): DataElementStatus | null {
  switch (status) {
    case TilsagnStatus.RETURNERT:
    case TilsagnStatus.TIL_GODKJENNING:
      return null;
    case TilsagnStatus.GODKJENT:
      return { variant: DataElementStatusVariant.SUCCESS, value: "Godkjent", description: null };
    case TilsagnStatus.TIL_ANNULLERING:
      return {
        variant: DataElementStatusVariant.WARNING,
        value: "Til annullering",
        description: null,
      };
    case TilsagnStatus.ANNULLERT:
      return {
        variant: DataElementStatusVariant.ERROR_BORDER_STRIKETHROUGH,
        value: "Annullert",
        description: null,
      };
    case TilsagnStatus.TIL_OPPGJOR:
      return { variant: DataElementStatusVariant.WARNING, value: "Til oppgjør", description: null };
    case TilsagnStatus.OPPGJORT:
      return { variant: DataElementStatusVariant.NEUTRAL, value: "Oppgjort", description: null };
  }
}
