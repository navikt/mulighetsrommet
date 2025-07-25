import { http, HttpResponse, PathParams } from "msw";
import { NavEnhetDto, NavRegionDto } from "@mr/api-client-v2";
import { mockEnheter, mockRegioner } from "../../fixtures/mockEnheter";

export const enhetHandlers = [
  http.get<PathParams, NavEnhetDto[]>("*/api/v1/intern/nav-enheter", () =>
    HttpResponse.json(Object.values(mockEnheter)),
  ),

  http.get<PathParams, NavRegionDto[]>("*/api/v1/intern/nav-enheter/regioner", () =>
    HttpResponse.json(Object.values(mockRegioner)),
  ),

  http.get<PathParams, NavEnhetDto[]>(
    "*/api/v1/intern/nav-enheter/:enhetsnummer/overordnet",
    ({ params }) => {
      const { enhetsnummer } = params;
      const overordnetEnhetsnummer = mockEnheter[`_${enhetsnummer}`]?.overordnetEnhet;
      const overordnetEnhet = mockEnheter[`_${overordnetEnhetsnummer}`];
      return HttpResponse.json(overordnetEnhet);
    },
  ),
];
