import { HttpResponse, PathParams, delay, http } from "msw";
import { UtkastDto as Utkast } from "mulighetsrommet-api-client";
import { mockUtkast } from "../fixtures/mock_utkast";

export const utkastHandlers = [
  http.put<PathParams, Utkast>("*/api/v1/internal/utkast", async ({ request }) => {
    const data = (await request.json()) as Utkast;

    const lagretUtkastIndex = mockUtkast.findIndex((ut) => ut.id === data.id);

    let payload: Utkast = {
      ...data,
      updatedAt: new Date().toISOString(),
    };

    if (lagretUtkastIndex > -1) {
      const lagretUtkast = mockUtkast[lagretUtkastIndex];
      payload = {
        ...lagretUtkast,
        ...payload,
        utkastData: {
          ...data.utkastData,
        },
      };
      mockUtkast[lagretUtkastIndex] = payload;
    } else {
      mockUtkast.push(data);
    }

    await delay(); // Simuler delay fra server
    return HttpResponse.json({ ...payload });
  }),

  http.delete<PathParams, Utkast[]>("*/api/v1/internal/utkast/:id", ({ params }) => {
    const { id } = params;
    const updated = mockUtkast.filter((ut) => ut.id !== id);
    return HttpResponse.json(updated);
  }),
  http.get<PathParams, Utkast[]>("*/api/v1/internal/utkast/mine", ({ request }) => {
    const url = new URL(request.url);
    const utkasttype = url.searchParams.get("utkasttype");
    return HttpResponse.json(mockUtkast.filter((utkast) => utkast.type === utkasttype));
  }),
  http.get<PathParams, Utkast | undefined>("*/api/v1/internal/utkast/:id", ({ params }) => {
    const found = [...mockUtkast].find((utkast) => utkast.id === params.id);
    return HttpResponse.json(found);
  }),
];
