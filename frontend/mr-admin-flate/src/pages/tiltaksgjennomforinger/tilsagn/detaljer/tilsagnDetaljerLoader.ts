import { AnsattService, TilsagnService, TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function tilsagnDetaljerLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId, tilsagnId } = params;

  if (!tiltaksgjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }

  if (!tilsagnId) {
    throw new Error("tilsagnId is missing");
  }

  const [ansatt, gjennomforing, tilsagn, historikk] = await Promise.all([
    AnsattService.hentInfoOmAnsatt(),
    TiltaksgjennomforingerService.getTiltaksgjennomforing({
      id: tiltaksgjennomforingId,
    }),
    TilsagnService.getTilsagn({ id: tilsagnId }),
    TilsagnService.getTilsagnEndringshistorikk({ id: tilsagnId }),
  ]);

  return { ansatt, gjennomforing, tilsagn, historikk };
}
