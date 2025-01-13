import {
  TilsagnService,
  TiltaksgjennomforingerService,
  AvtalerService,
  Avtaletype,
  TilsagnType,
} from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function tilsagnForGjennomforingLoader({ params }: LoaderFunctionArgs) {
  const { tiltaksgjennomforingId: gjennomforingId } = params;

  if (!gjennomforingId) {
    throw new Error("tiltaksgjennomforingId is missing");
  }

  const gjennomforing = await TiltaksgjennomforingerService.getTiltaksgjennomforing({
    id: gjennomforingId,
  });

  const [avtale, tilsagnForGjennomforing] = await Promise.all([
    AvtalerService.getAvtale({ id: gjennomforing.avtaleId! }),
    TilsagnService.getAll({ gjennomforingId }),
  ]);

  const tilsagnstyper =
    avtale.avtaletype === Avtaletype.FORHAANDSGODKJENT
      ? [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN, TilsagnType.INVESTERING]
      : [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN];

  return { tilsagnstyper, tilsagnForGjennomforing };
}
