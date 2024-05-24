import { HttpResponse, PathParams, http } from "msw";
import { NavEnhet, NavRegion } from "mulighetsrommet-api-client";
import { mockEnheter, mockRegioner } from "../../fixtures/mockEnheter";

export const enhetHandlers = [
  http.get<PathParams, NavEnhet[]>("*/api/v1/intern/nav-enheter", () =>
    HttpResponse.json(Object.values(mockEnheter)),
  ),

  http.get<PathParams, NavRegion[]>("*/api/v1/intern/nav-enheter/regioner", () =>
    HttpResponse.json(Object.values(mockRegioner)),
  ),

  http.get<PathParams, NavEnhet[]>(
    "*/api/v1/intern/nav-enheter/:enhetsnummer/overordnet",
    ({ params }) => {
      const { enhetsnummer } = params;
      const overordnetEnhetsnummer = mockEnheter[`_${enhetsnummer}`]?.overordnetEnhet;
      const overordnetEnhet = mockEnheter[`_${overordnetEnhetsnummer}`];
      return HttpResponse.json(overordnetEnhet);
    },
  ),
];
