import { HttpResponse, PathParams, http } from "msw";
import { Avtale, PaginertAvtale } from "mulighetsrommet-api-client";
import { mockAvtaler } from "../fixtures/mock_avtaler";

export const avtaleHandlers = [
  http.get<PathParams, PaginertAvtale | undefined>("*/api/v1/internal/avtaler", ({ request }) => {
    const url = new URL(request.url);
    const avtalestatus = url.searchParams.get("avtalestatus");
    const data = mockAvtaler.filter(
      (a) => a.avtalestatus === avtalestatus || avtalestatus === null,
    );

    return HttpResponse.json({
      pagination: {
        currentPage: 1,
        pageSize: 15,
        totalCount: data.length,
      },
      data,
    });
  }),

  http.get<PathParams, PaginertAvtale | undefined>(
    "*/api/v1/internal/avtaler/mine",
    ({ request }) => {
      const url = new URL(request.url);
      const avtalestatus = url.searchParams.get("avtalestatus");
      const brukerident = "B123456";
      const data = mockAvtaler.filter(
        (a) =>
          (a.avtalestatus === avtalestatus || avtalestatus === null) &&
          a.administrator?.navIdent === brukerident,
      );

      return HttpResponse.json({
        pagination: {
          currentPage: 1,
          pageSize: 15,
          totalCount: data.length,
        },
        data,
      });
    },
  ),

  http.get<PathParams, Avtale | undefined>("*/api/v1/internal/avtaler/:id", ({ params }) => {
    const { id } = params;
    const avtale = mockAvtaler.find((a) => a.id === id) ?? undefined;
    return HttpResponse.json(avtale);
  }),

  http.get<PathParams, Avtale | undefined>("*/api/v1/internal/avtaler/skjema", ({ params }) => {
    const { id } = params;
    const avtale = mockAvtaler.find((a) => a.id === id) ?? undefined;
    return HttpResponse.json(avtale);
  }),

  http.delete("/api/v1/internal/avtaler/:id", () => {
    return HttpResponse.json();
  }),

  http.put("*/api/v1/internal/avtaler", () => {
    return HttpResponse.json({
      id: "d1f163b7-1a41-4547-af16-03fd4492b7ba",
    });
  }),
];
