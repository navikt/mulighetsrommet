import { http, HttpResponse, PathParams } from "msw";
import { AvtaleDto, PaginertAvtale, PrismodellDto } from "@mr/api-client-v2";
import {
  AvtaleHandling,
  AvtaltSatsDto,
  EndringshistorikkDto,
} from "@tiltaksadministrasjon/api-client";
import { mockAvtaler } from "../fixtures/mock_avtaler";
import { mockEndringshistorikkAvtaler } from "../fixtures/mock_endringshistorikk_avtaler";

export const avtaleHandlers = [
  http.get<PathParams, PaginertAvtale | undefined>(
    "*/api/tiltaksadministrasjon/prismodeller",
    () => {
      return HttpResponse.json([
        {
          type: "FORHANDSGODKJENT_PRIS_PER_MANEDSVERK",
          beskrivelse: "Fast sats per tiltaksplass per måned",
        },
      ]);
    },
  ),

  http.get<PathParams, AvtaleHandling[]>(
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

  http.get<PathParams, PaginertAvtale | undefined>("*/api/v1/intern/avtaler", ({ request }) => {
    const url = new URL(request.url);
    const avtalestatus = url.searchParams.get("avtalestatus");
    const data = mockAvtaler.filter((a) => a.status.type === avtalestatus || avtalestatus === null);

    return HttpResponse.json({
      pagination: {
        pageSize: 15,
        totalCount: data.length,
      },
      data,
    });
  }),

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

  http.delete("/api/v1/intern/avtaler/kontaktperson", () => {
    return HttpResponse.json();
  }),

  http.put("*/api/v1/intern/avtaler", () => {
    return HttpResponse.json(mockAvtaler[0]);
  }),

  http.get<PathParams, undefined, AvtaltSatsDto[]>(
    "/api/tiltaksadministrasjon/prismodeller/forhandsgodkjente-satser",
    () => {
      return HttpResponse.json([
        {
          gjelderFra: "2025-01-01",
          gjelderTil: null,
          pris: 23000,
          valuta: "NOK",
        },
      ]);
    },
  ),

  http.get<PathParams, undefined, PrismodellDto[]>(
    "/api/tiltaksadministrasjon/prismodeller",
    () => {
      return HttpResponse.json([]);
    },
  ),

  http.get<PathParams, undefined, EndringshistorikkDto>(
    "*/api/tiltaksadministrasjon/avtaler/:id/historikk",
    () => {
      return HttpResponse.json(mockEndringshistorikkAvtaler);
    },
  ),
];
