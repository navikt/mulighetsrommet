import { AnsattService, TilsagnService, GjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function tilsagnDetaljerLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId, tilsagnId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }

  if (!tilsagnId) {
    throw new Error("tilsagnId is missing");
  }

  const [ansatt, gjennomforing, tilsagn, historikk] = await Promise.all([
    AnsattService.hentInfoOmAnsatt(),
    GjennomforingerService.getGjennomforing({
      id: gjennomforingId,
    }),
    TilsagnService.getTilsagn({ id: tilsagnId }),
    TilsagnService.getTilsagnEndringshistorikk({ id: tilsagnId }),
  ]);

  return { ansatt, gjennomforing, tilsagn, historikk };
}
