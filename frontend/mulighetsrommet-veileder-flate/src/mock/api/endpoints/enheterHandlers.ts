import { http, HttpResponse, PathParams } from "msw";
import { NavEnhetDto, NavRegionDto } from "@api-client";
import { mockEnheter, mockRegioner } from "../../fixtures/mockEnheter";

export const enhetHandlers = [
  http.get<PathParams, NavRegionDto[]>("*/api/veilederflate/nav-enheter/regioner", () =>
    HttpResponse.json(Object.values(mockRegioner)),
  ),

  http.get<PathParams, NavEnhetDto[]>(
    "*/api/veilederflate/nav-enheter/:enhetsnummer/overordnet",
    ({ params }) => {
      const { enhetsnummer } = params;
      const overordnetEnhetsnummer = mockEnheter[`_${enhetsnummer}`]?.overordnetEnhet;
      const overordnetEnhet = mockEnheter[`_${overordnetEnhetsnummer}`];
      return HttpResponse.json(overordnetEnhet);
    },
  ),
];
