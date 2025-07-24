import { http, HttpResponse, PathParams } from "msw";
import { NavEnhetDbo as NavEnhet, NavRegionDto as NavRegion } from "@api-client";
import { mockEnheter, mockRegioner } from "../../fixtures/mockEnheter";

export const enhetHandlers = [
  http.get<PathParams, NavEnhet[]>("*/api/veilederflate/nav-enheter", () =>
    HttpResponse.json(Object.values(mockEnheter)),
  ),

  http.get<PathParams, NavRegion[]>("*/api/veilederflate/nav-enheter/regioner", () =>
    HttpResponse.json(Object.values(mockRegioner)),
  ),

  http.get<PathParams, NavEnhet[]>(
    "*/api/veilederflate/nav-enheter/:enhetsnummer/overordnet",
    ({ params }) => {
      const { enhetsnummer } = params;
      const overordnetEnhetsnummer = mockEnheter[`_${enhetsnummer}`]?.overordnetEnhet;
      const overordnetEnhet = mockEnheter[`_${overordnetEnhetsnummer}`];
      return HttpResponse.json(overordnetEnhet);
    },
  ),
];
