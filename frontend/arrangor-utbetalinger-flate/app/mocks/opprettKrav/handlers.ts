import {
  ArrangorflateFilterType,
  ArrangorflateTiltakRadDto,
  OpprettKravData,
  PaginatedResponseArrangorflateTiltakRadDto,
} from "@arrangor-utbetalinger/api-client";
import { http, HttpResponse, PathParams } from "msw";
import { oversiktAktiveGjennomforinger } from "./gjennomforingMocks";
import { innsendingsInformasjon } from "./innsendingsInformasjonMocks";
import { steg } from "./opprettKravStegMocks";
import { utbetalingsInformasjon } from "./utbetalingsInformasjonMocks";
import { vedlegg } from "./vedleggMocks";

function opprettKravData(id: string): OpprettKravData {
  return {
    steg: steg[id],
    innsendingSteg: innsendingsInformasjon[id],
    utbetalingSteg: utbetalingsInformasjon[id],
    vedleggSteg: vedlegg[id],
  };
}

export const handlers = [
  http.get<PathParams, ArrangorflateTiltakRadDto[]>(
    "*/api-proxy/api/arrangorflate/tiltaksoversikt",
    ({ request }) => {
      const type = new URL(request.url).searchParams.get("type");
      if (type === ArrangorflateFilterType.AKTIVE) {
        return HttpResponse.json<PaginatedResponseArrangorflateTiltakRadDto>(
          oversiktAktiveGjennomforinger,
        );
      }
      return HttpResponse.json<PaginatedResponseArrangorflateTiltakRadDto>({
        pagination: { totalCount: 0, pageSize: 25, totalPages: 1 },
        data: [],
      });
    },
  ),
  http.get<PathParams, OpprettKravData>(
    "*/api-proxy/api/arrangorflate/arrangor/:orgnr/gjennomforing/:gjennomforingId/opprett-krav",
    ({ params }) => {
      const { gjennomforingId } = params;
      return HttpResponse.json<OpprettKravData>(opprettKravData(gjennomforingId as string));
    },
  ),
];
