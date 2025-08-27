import { http, HttpResponse, PathParams } from "msw";
import {
  AvtaleDto,
  AvtaltSatsDto,
  EndringshistorikkEntry,
  PaginertAvtale,
  PrismodellDto,
} from "@mr/api-client-v2";
import { mockAvtaler } from "../fixtures/mock_avtaler";
import { mockEndringshistorikkAvtaler } from "../fixtures/mock_endringshistorikk_avtaler";

export const avtaleHandlers = [
  http.get<PathParams, PaginertAvtale | undefined>("*/api/v1/intern/prismodeller", () => {
    return HttpResponse.json([
      {
        type: "FORHANDSGODKJENT_PRIS_PER_MANEDSVERK",
        beskrivelse: "Fast sats per tiltaksplass per måned",
      },
    ]);
  }),

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

  http.get<PathParams, PaginertAvtale | undefined>(
    "*/api/v1/intern/avtaler/mine",
    ({ request }) => {
      const url = new URL(request.url);
      const avtalestatus = url.searchParams.get("avtalestatus");
      const brukerident = "B123456";
      const data = mockAvtaler.filter(
        (a) =>
          (a.status.type === avtalestatus || avtalestatus === null) &&
          a.administratorer?.map((admin) => admin.navIdent).includes(brukerident),
      );

      return HttpResponse.json({
        pagination: {
          pageSize: 15,
          totalCount: data.length,
        },
        data,
      });
    },
  ),

  http.put<{ id: string }, number>("*/api/v1/intern/avtaler/:id/avbryt", () => {
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

  http.get<PathParams, AvtaltSatsDto | undefined>(
    "/api/v1/intern/prismodeller/forhandsgodkjente-satser",
    () => {
      return HttpResponse.json([
        {
          periodeStart: "2025-01-01",
          periodeSlutt: "2025-12-01",
          pris: 23000,
          valuta: "NOK",
        },
      ]);
    },
  ),

  http.get<PathParams, PrismodellDto | undefined>("/api/v1/intern/prismodeller", () => {
    return HttpResponse.json([]);
  }),

  http.get<PathParams, EndringshistorikkEntry>("*/api/v1/intern/avtaler/:id/historikk", () => {
    return HttpResponse.json(mockEndringshistorikkAvtaler);
  }),
];
