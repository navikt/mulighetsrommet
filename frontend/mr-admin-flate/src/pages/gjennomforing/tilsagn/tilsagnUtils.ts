import { navnEllerIdent } from "@/utils/Utils";
import {
  AgentDto,
  TilsagnBeregningAvtaltPrisPerManedsverk,
  TilsagnBeregningFri,
  TilsagnDto,
} from "@mr/api-client-v2";

export function isTilsagnAvtaltPrisPerManedsverk(
  tilsagn: TilsagnDto,
): tilsagn is TilsagnDto & { beregning: TilsagnBeregningAvtaltPrisPerManedsverk } {
  return tilsagn.beregning.type === "AVTALT_PRIS_PER_MANEDSVERK";
}

export function isTilsagnFri(
  tilsagn: TilsagnDto,
): tilsagn is TilsagnDto & { beregning: TilsagnBeregningFri } {
  return tilsagn.beregning.type === "FRI";
}

export const navnIdentEllerPlaceholder = (agent?: AgentDto) =>
  agent ? navnEllerIdent(agent) : "-";
