import { http, HttpResponse, PathParams } from "msw";
import {
  AvtaleDto,
  AvtaleHandling,
  EndringshistorikkDto,
  PaginatedResponseAvtaleDto,
  PrismodellInfo,
  PrismodellType,
} from "@tiltaksadministrasjon/api-client";
import { mockAvtaler } from "../fixtures/mock_avtaler";
import { mockEndringshistorikkAvtaler } from "../fixtures/mock_endringshistorikk_avtaler";

export const avtaleHandlers = [
  http.get<PathParams, undefined, PrismodellInfo[]>(
    "*/api/tiltaksadministrasjon/prismodeller",
    () => {
      return HttpResponse.json([
        {
          type: PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
          beskrivelse: "Fast sats per tiltaksplass per måned",
        },
      ]);
    },
  ),

  http.get<PathParams, undefined, AvtaleHandling[]>(
    "*/api/tiltaksadministrasjon/avtaler/:id/handlinger",
    () => {
      return HttpResponse.json([
        AvtaleHandling.REDIGER,
        AvtaleHandling.AVBRYT,
        AvtaleHandling.OPPRETT_GJENNOMFORING,
        AvtaleHandling.DUPLISER,
        AvtaleHandling.REGISTRER_OPSJON,
        AvtaleHandling.OPPDATER_PRIS,
      ]);
    },
  ),

  http.get<PathParams, undefined, PaginatedResponseAvtaleDto>(
    "*/api/v1/intern/avtaler",
    ({ request }) => {
      const url = new URL(request.url);
      const avtalestatus = url.searchParams.get("avtalestatus");
      const data = mockAvtaler.filter(
        (a) => a.status.type === avtalestatus || avtalestatus === null,
      );

      return HttpResponse.json({
        pagination: {
          pageSize: 15,
          totalCount: data.length,
          totalPages: 1,
        },
        data,
      });
    },
  ),

  http.put<{ id: string }, number>("*/api/tiltaksadministrasjon/avtaler/:id/avbryt", () => {
    return HttpResponse.json(1);
  }),

  http.get<PathParams, AvtaleDto | undefined>("*/api/v1/intern/avtaler/:id", ({ params }) => {
    const { id } = params;
    const avtale = mockAvtaler.find((a) => a.id === id) ?? undefined;
    return HttpResponse.json(avtale);
  }),

  http.get<PathParams, AvtaleDto | undefined>("*/api/v1/intern/avtaler/skjema", ({ params }) => {
    const { id } = params;
    const avtale = mockAvtaler.find((a) => a.id === id) ?? undefined;
    return HttpResponse.json(avtale);
  }),

  http.put("*/api/v1/intern/avtaler", () => {
    return HttpResponse.json(mockAvtaler[0]);
  }),

  http.get<PathParams, undefined, EndringshistorikkDto>(
    "*/api/tiltaksadministrasjon/avtaler/:id/historikk",
    () => {
      return HttpResponse.json(mockEndringshistorikkAvtaler);
    },
  ),
];
