import {
  TiltaksoversiktType,
  OpprettKravDeltakere,
  ArrangorInnsendingRadDto,
  OpprettKravData,
} from "@api-client";
import { http, HttpResponse, PathParams } from "msw";
import { oversiktAktiveGjennomforinger } from "./gjennomforingMocks";
import { innsendingsInformasjon } from "./innsendingsInformasjonMocks";
import { steg } from "./opprettKravStegMocks";
import { utbetalingsInformasjon } from "./utbetalingsInformasjonMocks";
import { vedlegg } from "./vedleggMocks";
import { deltakere } from "./deltakelserMocks";

function opprettKravData(id: string): OpprettKravData {
  return {
    steg: steg[id],
    innsendingSteg: innsendingsInformasjon[id],
    utbetalingSteg: utbetalingsInformasjon[id],
    vedleggSteg: vedlegg[id],
  };
}

export const handlers = [
  http.get<PathParams, ArrangorInnsendingRadDto[]>(
    "*/api-proxy/api/arrangorflate/tiltaksoversikt",
    ({ request }) => {
      const type = new URL(request.url).searchParams.get("type");
      if (type === TiltaksoversiktType.AKTIVE) {
        return HttpResponse.json<ArrangorInnsendingRadDto[]>(oversiktAktiveGjennomforinger);
      }
      return HttpResponse.json<ArrangorInnsendingRadDto[]>([]);
    },
  ),
  http.get<PathParams, OpprettKravData>(
    "*/api-proxy/api/arrangorflate/arrangor/:orgnr/gjennomforing/:gjennomforingId/opprett-krav",
    ({ params }) => {
      const { gjennomforingId } = params;
      return HttpResponse.json<OpprettKravData>(opprettKravData(gjennomforingId as string));
    },
  ),
  http.get<PathParams, OpprettKravDeltakere>(
    "*/api-proxy/api/arrangorflate/arrangor/:orgnr/gjennomforing/:gjennomforingId/opprett-krav/deltakere",
    ({ params }) => {
      const { gjennomforingId } = params;
      return HttpResponse.json<OpprettKravDeltakere>(deltakere[gjennomforingId as string]);
    },
  ),
];
