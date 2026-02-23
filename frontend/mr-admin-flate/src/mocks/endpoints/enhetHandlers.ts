import { http, HttpResponse, PathParams } from "msw";
import { kontorstruktur, kostnadssteder } from "../fixtures/mock_enheter";
import { Kontorstruktur, RegionKostnadssteder } from "@tiltaksadministrasjon/api-client";

export const enhetHandlers = [
  http.get<PathParams, RegionKostnadssteder[]>(
    "*/api/tiltaksadministrasjon/kodeverk/kostnadssteder",
    () => HttpResponse.json(Object.values(kostnadssteder)),
  ),

  http.get<PathParams, Kontorstruktur[]>(
    "*/api/tiltaksadministrasjon/kodeverk/kontorstruktur",
    () => HttpResponse.json(Object.values(kontorstruktur)),
  ),
];
