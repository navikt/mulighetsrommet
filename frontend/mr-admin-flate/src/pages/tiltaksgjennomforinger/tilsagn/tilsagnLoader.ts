import { AnsattService, TilsagnService, TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function tilsagnLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId, tilsagnId } = params;
  const tiltaksgjennomforing = tiltaksgjennomforingId
    ? await TiltaksgjennomforingerService.getTiltaksgjennomforing({ id: tiltaksgjennomforingId })
    : undefined;
  const tilsagn = tilsagnId ? await TilsagnService.getTilsagn({ id: tilsagnId }) : undefined;
  const tilsagnForGjennomforing = tiltaksgjennomforing?.id
    ? await TilsagnService.tilsagnByTiltaksgjennomforing({
        tiltaksgjennomforingId: tiltaksgjennomforing.id,
      })
    : undefined;
  const ansatt = await AnsattService.hentInfoOmAnsatt();

  return { tiltaksgjennomforing, tilsagn, tilsagnForGjennomforing, ansatt };
}
