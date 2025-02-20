import { Avtaletype, TilsagnService, TilsagnType } from "@mr/api-client-v2";
import { QueryClient } from "@tanstack/react-query";
import { LoaderFunctionArgs } from "react-router";
import { avtaleQuery } from "../../../avtaler/avtaleLoader";
import { gjennomforingQuery } from "../../gjennomforingLoaders";

export const tilsagnForGjennomforingLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const { gjennomforingId } = params;

    if (!gjennomforingId) {
      throw new Error("gjennomforingId is missing");
    }

    const { data: gjennomforing } = await queryClient.ensureQueryData(
      gjennomforingQuery(gjennomforingId),
    );

    const [{ data: avtale }, { data: tilsagnForGjennomforing }] = await Promise.all([
      queryClient.ensureQueryData(avtaleQuery(gjennomforing.avtaleId!)),
      TilsagnService.getAll({ query: { gjennomforingId } }),
    ]);

    const tilsagnstyper =
      avtale.avtaletype === Avtaletype.FORHAANDSGODKJENT
        ? [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN, TilsagnType.INVESTERING]
        : [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN];

    return { tilsagnstyper, tilsagnForGjennomforing };
  };
