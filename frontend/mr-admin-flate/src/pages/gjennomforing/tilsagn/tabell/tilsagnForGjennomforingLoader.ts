import {
  TilsagnService,
  AvtalerService,
  Avtaletype,
  TilsagnType,
  GjennomforingerService,
} from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function tilsagnForGjennomforingLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId: gjennomforingId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }

  const { data: gjennomforing } = await GjennomforingerService.getGjennomforing({
    path: { id: gjennomforingId },
  });

  const [{ data: avtale }, { data: tilsagnForGjennomforing }] = await Promise.all([
    AvtalerService.getAvtale({ path: { id: gjennomforing.avtaleId! } }),
    TilsagnService.getAll({ query: { gjennomforingId } }),
  ]);

  const tilsagnstyper =
    avtale.avtaletype === Avtaletype.FORHAANDSGODKJENT
      ? [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN, TilsagnType.INVESTERING]
      : [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN];

  return { tilsagnstyper, tilsagnForGjennomforing };
}
