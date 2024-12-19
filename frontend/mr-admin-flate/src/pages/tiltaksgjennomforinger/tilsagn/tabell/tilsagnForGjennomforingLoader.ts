import { TilsagnService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function tilsagnForGjennomforingLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId } = params;

  if (!tiltaksgjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }

  const [tilsagnForGjennomforing] = await Promise.all([
    TilsagnService.tilsagnByTiltaksgjennomforing({
      tiltaksgjennomforingId,
    }),
  ]);

  return { tilsagnForGjennomforing };
}
