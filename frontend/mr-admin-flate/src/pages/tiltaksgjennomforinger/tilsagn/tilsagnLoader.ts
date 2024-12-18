import { AnsattService, TilsagnService, TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function tilsagnLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId, tilsagnId } = params;

  if (!tiltaksgjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }
  const tiltaksgjennomforing = await TiltaksgjennomforingerService.getTiltaksgjennomforing({
    id: tiltaksgjennomforingId,
  });

  const tilsagn = tilsagnId ? await TilsagnService.getTilsagn({ id: tilsagnId }) : undefined;

  const tilsagnForGjennomforing = await TilsagnService.tilsagnByTiltaksgjennomforing({
    tiltaksgjennomforingId: tiltaksgjennomforing.id,
  });

  const ansatt = await AnsattService.hentInfoOmAnsatt();

  const historikk = tilsagnId
    ? await TilsagnService.getTilsagnEndringshistorikk({ id: tilsagnId })
    : { entries: [] };

  return { tiltaksgjennomforing, tilsagn, tilsagnForGjennomforing, ansatt, historikk };
}
