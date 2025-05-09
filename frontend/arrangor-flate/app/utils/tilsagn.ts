import { ArrangorflateTilsagn } from "api-client"

export const gjenstaendeBelop = (tilsagn: ArrangorflateTilsagn) => {
  if (tilsagn.status.status === 'OPPGJORT' || tilsagn.status.status === 'ANNULLERT') {
    return 0
  }
  return tilsagn.beregning.output.belop - tilsagn.bruktBelop
}