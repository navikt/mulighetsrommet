import { RefusjonskravService, GjennomforingerService } from "@mr/api-client";
import { LoaderFunctionArgs } from "react-router";

export async function refusjonskravDetaljerLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId, refusjonskravId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }

  if (!refusjonskravId) {
    throw new Error("refusjonskravId is missing");
  }

  const [gjennomforing, refusjonskrav] = await Promise.all([
    GjennomforingerService.getGjennomforing({
      id: gjennomforingId,
    }),
    RefusjonskravService.getRefusjonskrav({ id: refusjonskravId }),
  ]);

  return { gjennomforing, refusjonskrav };
}
