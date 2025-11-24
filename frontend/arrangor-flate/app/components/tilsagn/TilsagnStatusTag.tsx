import { DataElement, DataElementStatusVariant, TilsagnStatus } from "api-client";

export function tilsagnStatusElement(status: TilsagnStatus): DataElement | null {
  switch (status) {
    case TilsagnStatus.RETURNERT:
    case TilsagnStatus.TIL_GODKJENNING:
      return null;
    case TilsagnStatus.GODKJENT:
      return {
        variant: DataElementStatusVariant.SUCCESS,
        value: "Godkjent",
        type: "DATA_ELEMENT_STATUS",
        description: null,
      };
    case TilsagnStatus.TIL_ANNULLERING:
      return {
        variant: DataElementStatusVariant.WARNING,
        value: "Til annullering",
        type: "DATA_ELEMENT_STATUS",
        description: null,
      };
    case TilsagnStatus.ANNULLERT:
      return {
        variant: DataElementStatusVariant.ERROR_BORDER_STRIKETHROUGH,
        value: "Annullert",
        type: "DATA_ELEMENT_STATUS",
        description: null,
      };
    case TilsagnStatus.TIL_OPPGJOR:
      return {
        type: "DATA_ELEMENT_STATUS",
        variant: DataElementStatusVariant.WARNING,
        value: "Til oppgjør",
        description: null,
      };
    case TilsagnStatus.OPPGJORT:
      return {
        type: "DATA_ELEMENT_STATUS",
        variant: DataElementStatusVariant.NEUTRAL,
        value: "Oppgjort",
        description: null,
      };
  }
}
