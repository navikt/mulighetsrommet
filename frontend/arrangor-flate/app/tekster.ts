import { TilsagnType } from "@mr/api-client-v2";

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
