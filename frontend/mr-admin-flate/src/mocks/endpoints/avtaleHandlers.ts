import { http, HttpResponse, PathParams } from "msw";
import {
  AvtaleDto,
  AvtaleHandling,
  Avtaletype,
  AvtaletypeInfo,
  EndringshistorikkDto,
  PaginatedResponseAvtaleDto,
  PrismodellInfo,
  PrismodellType,
} from "@tiltaksadministrasjon/api-client";
import { mockAvtaler } from "../fixtures/mock_avtaler";
import { mockEndringshistorikkAvtaler } from "../fixtures/mock_endringshistorikk_avtaler";

export const avtaleHandlers = [
  http.get<PathParams, undefined, AvtaletypeInfo[]>(
    "*/api/tiltaksadministrasjon/avtaletyper",
    () => {
      return HttpResponse.json([
        {
          type: Avtaletype.FORHANDSGODKJENT,
          tittel: "Forhåndsgodkjent",
        },
        {
          type: Avtaletype.RAMMEAVTALE,
          tittel: "Rammeavtale",
        },
        {
          type: Avtaletype.AVTALE,
          tittel: "Avtale",
        },
        {
          type: Avtaletype.OFFENTLIG_OFFENTLIG,
          tittel: "Offentlig-offentlig samarbeid",
        },
      ]);
    },
  ),

  http.get<PathParams, undefined, PrismodellInfo[]>(
    "*/api/tiltaksadministrasjon/prismodeller",
    () => {
      return HttpResponse.json([
        {
          type: PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
          navn: "Fast sats per tiltaksplass per måned",
          beskrivelse: [],
        },
      ]);
    },
  ),

  http.get<PathParams, undefined, AvtaleHandling[]>(
    "*/api/tiltaksadministrasjon/avtaler/handlinger",
    () => {
      return HttpResponse.json([AvtaleHandling.OPPRETT]);
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

  http.post<PathParams, undefined, PaginatedResponseAvtaleDto>(
    "*/api/tiltaksadministrasjon/avtaler",
    () => {
      const data = mockAvtaler;
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

  http.post("*/api/tiltaksadministrasjon/avtaler/excel", () => {
    return new HttpResponse(new Blob(["mock excel"]), {
      status: 200,
      headers: {
        "Content-Type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "Content-Disposition": 'attachment; filename="avtaler.xlsx"',
      },
    });
  }),

  http.put<{ id: string }, number>("*/api/tiltaksadministrasjon/avtaler/:id/avbryt", () => {
    return HttpResponse.json(1);
  }),

  http.get<PathParams, undefined, AvtaleDto>(
    "*/api/tiltaksadministrasjon/avtaler/:id",
    ({ params }) => {
      const { id } = params;
      const avtale = mockAvtaler.find((a) => a.id === id) ?? undefined;
      return HttpResponse.json(avtale);
    },
  ),

  http.put("*/api/tiltaksadministrasjon/avtaler", () => {
    return HttpResponse.json(mockAvtaler[0]);
  }),

  http.get<PathParams, undefined, EndringshistorikkDto>(
    "*/api/tiltaksadministrasjon/avtaler/:id/historikk",
    () => {
      return HttpResponse.json(mockEndringshistorikkAvtaler);
    },
  ),
];
