import { HttpResponse, http } from "msw";
import { AvtaleNotat, AvtaleNotatRequest } from "mulighetsrommet-api-client";
import { mockAvtalenotater } from "../fixtures/mock_avtalenotater";

let avtalenotater = [...mockAvtalenotater];

export const avtalenotatHandlers = [
  http.get<any, any, AvtaleNotat[]>("*/api/v1/internal/notater/avtaler", () =>
    HttpResponse.json(avtalenotater.sort(sortByDate)),
  ),
  http.get<any, any, AvtaleNotat[]>("*/api/v1/internal/notater/avtaler/mine", () =>
    HttpResponse.json(
      avtalenotater.filter((notat) => notat.opprettetAv.navIdent === "B123456").sort(sortByDate),
    ),
  ),

  http.put<AvtaleNotatRequest, any, any>(
    "*/api/v1/internal/notater/avtaler",
    async ({ request }) => {
      const payload = (await request.json()) as AvtaleNotatRequest;
      avtalenotater.push({
        ...payload,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        opprettetAv: {
          navIdent: "B123456",
          navn: "Bertil Bengtson",
        },
      });
      return HttpResponse.json();
    },
  ),

  http.delete<any, any, any>("*/api/v1/internal/notater/avtaler/:id", ({ params }) => {
    const { id } = params;
    avtalenotater = [...avtalenotater.filter((notat) => notat.id !== id)];
    return HttpResponse.json();
  }),
];

function sortByDate(a: AvtaleNotat, b: AvtaleNotat) {
  return new Date(b.createdAt).valueOf() - new Date(a.createdAt).valueOf();
}
