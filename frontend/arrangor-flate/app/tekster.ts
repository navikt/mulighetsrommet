import { TilsagnType } from "api-client";

export const tekster = {
  tilsagn: {
    tilsagntype: (type: TilsagnType) => {
      switch (type) {
        case TilsagnType.TILSAGN:
          return "Tilsagn";
        case TilsagnType.EKSTRATILSAGN:
          return "Ekstratilsagn";
        case TilsagnType.INVESTERING:
          return "Tilsagn for investeringer";
      }
    },
  },
};
