import { HttpResponse, PathParams, http } from "msw";
import { TilsagnRequest } from "mulighetsrommet-api-client";

export const tilsagnHandlers = [
  http.put<PathParams, TilsagnRequest>(
    "*/api/v1/intern/tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn",
    async ({ request }) => {
      const body = await request.json();
      return HttpResponse.json(body);
    },
  ),
];
