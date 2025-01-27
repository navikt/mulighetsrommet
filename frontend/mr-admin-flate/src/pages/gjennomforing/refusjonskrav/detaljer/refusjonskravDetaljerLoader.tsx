import { RefusjonskravService, GjennomforingerService } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

export async function refusjonskravDetaljerLoader({ params }: LoaderFunctionArgs) {
  const { gjennomforingId, refusjonskravId } = params;

  if (!gjennomforingId) {
    throw new Error("gjennomforingId is missing");
  }

  if (!refusjonskravId) {
    throw new Error("refusjonskravId is missing");
  }

  const [{ data: gjennomforing }, { data: refusjonskrav }] = await Promise.all([
    GjennomforingerService.getGjennomforing({
      path: { id: gjennomforingId },
    }),
    RefusjonskravService.getRefusjonskrav({ path: { id: refusjonskravId } }),
  ]);

  return { gjennomforing, refusjonskrav };
}
