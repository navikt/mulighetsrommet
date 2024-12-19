import { TilsagnService, TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function redigerTilsagnLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId, tilsagnId } = params;

  if (!tiltaksgjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }

  if (!tilsagnId) {
    throw new Error("tilsagnId is missing");
  }

  const [gjennomforing, tilsagn, alleTilsagn] = await Promise.all([
    TiltaksgjennomforingerService.getTiltaksgjennomforing({
      id: tiltaksgjennomforingId,
    }),
    TilsagnService.getTilsagn({ id: tilsagnId }),
    TilsagnService.tilsagnByTiltaksgjennomforing({
      tiltaksgjennomforingId,
    }),
  ]);

  // TODO: get by status og flytt til backend....
  const godkjenteTilsagn = alleTilsagn.filter((d) => d.status.type === "GODKJENT");

  return { gjennomforing, tilsagn, godkjenteTilsagn };
}
