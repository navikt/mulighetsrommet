import { TilsagnService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function tilsagnForGjennomforingLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId: gjennomforingId } = params;

  if (!gjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }

  const tilsagnForGjennomforing = await TilsagnService.getAll({ gjennomforingId });

  return { tilsagnForGjennomforing };
}
