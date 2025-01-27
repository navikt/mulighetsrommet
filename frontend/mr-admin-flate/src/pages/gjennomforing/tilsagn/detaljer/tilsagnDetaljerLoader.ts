import { AnsattService, TilsagnService, GjennomforingerService } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function tilsagnDetaljerLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId, tilsagnId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }

  if (!tilsagnId) {
    throw new Error("tilsagnId is missing");
  }

  const [{ data: ansatt }, { data: gjennomforing }, { data: tilsagn }, { data: historikk }] =
    await Promise.all([
      AnsattService.hentInfoOmAnsatt(),
      GjennomforingerService.getGjennomforing({
        path: { id: gjennomforingId },
      }),
      TilsagnService.getTilsagn({ path: { id: tilsagnId } }),
      TilsagnService.getTilsagnEndringshistorikk({ path: { id: tilsagnId } }),
    ]);

  return { ansatt, gjennomforing, tilsagn, historikk };
}
