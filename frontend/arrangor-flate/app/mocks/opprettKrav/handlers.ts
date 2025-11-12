import {
  GjennomforingerTableResponse,
  OpprettKravDeltakere,
  OpprettKravInnsendingsInformasjon,
  OpprettKravOppsummering,
  OpprettKravOppsummeringRequest,
  OpprettKravUtbetalingsinformasjon,
  OpprettKravVedlegg,
  OpprettKravVeiviserMeta,
} from "@api-client";
import { http, HttpResponse, PathParams } from "msw";
import { oversiktAktiveGjennomforinger } from "./gjennomforingMocks";
import { innsendingsInformasjon } from "./innsendingsInformasjonMocks";
import { veiviserMeta } from "./opprettKravStegMocks";
import { utbetalingsInformasjon } from "./utbetalingsInformasjonMocks";
import { vedlegg } from "./vedleggMocks";
import { oppsummering, utbetalingMap } from "./oppsummeringMocks";
import { pathByOrgnr } from "~/utils/navigation";
import { deltakere } from "./deltakelserMocks";

export const handlers = [
  http.get<PathParams, GjennomforingerTableResponse[]>(
    "*/api/arrangorflate/arrangor/:orgnr/gjennomforing/opprett-krav",
    () =>
      HttpResponse.json<GjennomforingerTableResponse>({
        aktive: oversiktAktiveGjennomforinger,
        historiske: { columns: [], rows: [] },
      }),
  ),
  http.get<PathParams, GjennomforingerTableResponse[]>(
    "*/api/arrangorflate/arrangor/:orgnr/gjennomforing/:gjennomforingId/opprett-krav",
    ({ params }) => {
      const { gjennomforingId } = params;
      return HttpResponse.json<OpprettKravVeiviserMeta>(veiviserMeta[gjennomforingId as string]);
    },
  ),
  http.get<PathParams, OpprettKravInnsendingsInformasjon>(
    "*/api/arrangorflate/arrangor/:orgnr/gjennomforing/:gjennomforingId/opprett-krav/innsendingsinformasjon",
    ({ params }) => {
      const { gjennomforingId } = params;
      return HttpResponse.json<OpprettKravInnsendingsInformasjon>(
        innsendingsInformasjon[gjennomforingId as string],
      );
    },
  ),
  http.get<PathParams, OpprettKravDeltakere>(
    "*/api/arrangorflate/arrangor/:orgnr/gjennomforing/:gjennomforingId/opprett-krav/deltakere",
    ({ params }) => {
      const { gjennomforingId } = params;
      return HttpResponse.json<OpprettKravDeltakere>(deltakere[gjennomforingId as string]);
    },
  ),
  http.get<PathParams, OpprettKravUtbetalingsinformasjon>(
    "*/api/arrangorflate/arrangor/:orgnr/gjennomforing/:gjennomforingId/opprett-krav/utbetalingsinformasjon",
    ({ params }) => {
      const { gjennomforingId } = params;
      return HttpResponse.json<OpprettKravUtbetalingsinformasjon>(
        utbetalingsInformasjon[gjennomforingId as string],
      );
    },
  ),
  http.get<PathParams, OpprettKravUtbetalingsinformasjon>(
    "*/api/arrangorflate/arrangor/:orgnr/gjennomforing/:gjennomforingId/opprett-krav/vedlegg",
    ({ params }) => {
      const { gjennomforingId } = params;
      return HttpResponse.json<OpprettKravVedlegg>(vedlegg[gjennomforingId as string]);
    },
  ),
  http.post<PathParams, OpprettKravOppsummeringRequest, OpprettKravOppsummering>(
    "*/api/arrangorflate/arrangor/:orgnr/gjennomforing/:gjennomforingId/opprett-krav/oppsummering",
    async ({ params, request }) => {
      const { gjennomforingId } = params;
      const requestBody = await request.json();
      return HttpResponse.json<OpprettKravOppsummering>(
        oppsummering[gjennomforingId as string](requestBody),
      );
    },
  ),

  http.post<PathParams, OpprettKravOppsummering>(
    "*/api/arrangorflate/arrangor/:orgnr/gjennomforing/:gjennomforingId/opprett-krav",
    ({ params }) => {
      const { orgnr, gjennomforingId } = params;

      return HttpResponse.redirect(
        pathByOrgnr(orgnr as string).kvittering(utbetalingMap[gjennomforingId as string]),
      );
    },
  ),
];
