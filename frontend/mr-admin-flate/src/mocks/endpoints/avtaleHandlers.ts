import { http, HttpResponse, PathParams } from "msw";
import { Avtale, EndringshistorikkEntry, PaginertAvtale } from "mulighetsrommet-api-client";
import { mockAvtaler } from "../fixtures/mock_avtaler";
import { mockEndringshistorikkAvtaler } from "../fixtures/mock_endringshistorikk_avtaler";

export const avtaleHandlers = [
  http.get<PathParams, PaginertAvtale | undefined>("*/api/v1/intern/avtaler", ({ request }) => {
    const url = new URL(request.url);
    const avtalestatus = url.searchParams.get("avtalestatus");
    const data = mockAvtaler.filter((a) => a.status.name === avtalestatus || avtalestatus === null);

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
          (a.status.name === avtalestatus || avtalestatus === null) &&
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

  http.put<{ id: string }, Number>("*/api/v1/intern/avtaler/:id/avbryt", () => {
    return HttpResponse.json(1);
  }),

  http.get<PathParams, Avtale | undefined>("*/api/v1/intern/avtaler/:id", ({ params }) => {
    const { id } = params;
    const avtale = mockAvtaler.find((a) => a.id === id) ?? undefined;
    return HttpResponse.json(avtale);
  }),

  http.get<PathParams, Avtale | undefined>("*/api/v1/intern/avtaler/skjema", ({ params }) => {
    const { id } = params;
    const avtale = mockAvtaler.find((a) => a.id === id) ?? undefined;
    return HttpResponse.json(avtale);
  }),

  http.delete("/api/v1/intern/avtaler/kontaktperson", () => {
    return HttpResponse.json();
  }),

  http.delete("/api/v1/intern/avtaler/:id", () => {
    return HttpResponse.json();
  }),

  http.put("*/api/v1/intern/avtaler", () => {
    return HttpResponse.json({
      id: "d1f163b7-1a41-4547-af16-03fd4492b7ba",
    });
  }),

  http.get<PathParams, EndringshistorikkEntry>("*/api/v1/intern/avtaler/:id/historikk", () => {
    return HttpResponse.json(mockEndringshistorikkAvtaler);
  }),
];
