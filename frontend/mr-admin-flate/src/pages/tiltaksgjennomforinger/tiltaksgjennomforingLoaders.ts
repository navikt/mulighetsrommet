import { TiltaksgjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router-dom";

export async function tiltaksgjennomforingLoader({ params }: LoaderFunctionArgs) {
  return params.tiltaksgjennomforingId
    ? await TiltaksgjennomforingerService.getTiltaksgjennomforing({
        id: params.tiltaksgjennomforingId,
      })
    : undefined;
}
