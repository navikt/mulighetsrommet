import { http, HttpResponse, PathParams } from "msw";
import { Arrangor, ArrangorKontaktperson, PaginertArrangor } from "@mr/api-client";
import { mockArrangorer } from "../fixtures/mock_arrangorer";
import { mockArrangorKontaktpersoner } from "../fixtures/mock_arrangorKontaktperson";
import { mockAvtaler } from "../fixtures/mock_avtaler";
import { mockTiltaksgjennomforinger } from "../fixtures/mock_tiltaksgjennomforinger";

export const arrangorHandlers = [
  http.get<PathParams, PaginertArrangor | undefined>("*/api/v1/intern/arrangorer", () =>
    HttpResponse.json(mockArrangorer),
  ),
  http.get<PathParams, Arrangor | undefined>("*/api/v1/intern/arrangorer/:id", ({ params }) => {
    return HttpResponse.json(mockArrangorer.data.find((enhet) => enhet.id === params.id));
  }),
  http.post<PathParams, Arrangor | undefined>("*/api/v1/intern/arrangorer/:id", () => {
    return HttpResponse.json(mockArrangorer.data[0]);
  }),
  http.get<PathParams, Arrangor | undefined>(
    "*/api/v1/intern/arrangorer/hovedenhet/:id",
    ({ params }) => {
      return HttpResponse.json(mockArrangorer.data.find((enhet) => enhet.id === params.id));
    },
  ),
  http.post<PathParams, Arrangor | undefined>("*/api/v1/intern/arrangorer/:orgnr", ({ params }) => {
    return HttpResponse.json(
      Object.values(mockArrangorer.data).find(
        (enhet) => enhet.organisasjonsnummer === params.orgnr,
      ),
    );
  }),
  http.get<PathParams, Arrangor | undefined>("*/api/v1/intern/arrangorer/kontaktperson/:id", () => {
    return HttpResponse.json({
      avtaler: [...mockAvtaler],
      gjennomforinger: [...mockTiltaksgjennomforinger],
    });
  }),
  http.get<PathParams, ArrangorKontaktperson[]>(
    "*/api/v1/intern/arrangorer/*/kontaktpersoner",
    () => HttpResponse.json(mockArrangorKontaktpersoner),
  ),
];
