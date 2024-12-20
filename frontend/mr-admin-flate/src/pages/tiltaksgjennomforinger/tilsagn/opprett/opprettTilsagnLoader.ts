import { TilsagnService, TilsagnType, TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function opprettTilsagnLoader({ params, request }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId } = params;

  if (!tiltaksgjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }

  const url = new URL(request.url);
  const type = (url.searchParams.get("type") as TilsagnType) ?? TilsagnType.TILSAGN;

  const [gjennomforing, defaults, alleTilsagn] = await Promise.all([
    TiltaksgjennomforingerService.getTiltaksgjennomforing({
      id: tiltaksgjennomforingId,
    }),
    TilsagnService.getTilsagnDefaults({
      gjennomforingId: tiltaksgjennomforingId,
      type,
    }),
    TilsagnService.tilsagnByTiltaksgjennomforing({
      tiltaksgjennomforingId,
    }),
  ]);

  // TODO: get by status og flytt til backend....
  const godkjenteTilsagn = alleTilsagn.filter((d) => d.status.type === "GODKJENT");

  return { gjennomforing, defaults, godkjenteTilsagn };
}
