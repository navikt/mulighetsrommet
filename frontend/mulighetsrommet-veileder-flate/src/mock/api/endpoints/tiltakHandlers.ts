import { http, HttpResponse } from "msw";
import {
  Innsatsgruppe,
  KontaktinfoVarsel,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { mockInnsatsgrupper } from "@/mock/fixtures/mockInnsatsgrupper";
import { mockTiltaksgjennomforinger } from "@/mock/fixtures/mockTiltaksgjennomforinger";
import { mockTiltakstyper } from "@/mock/fixtures/mockTiltakstyper";

export const tiltakHandlers = [
  http.get("*/api/v1/intern/veileder/innsatsgrupper", async () => {
    return HttpResponse.json(mockInnsatsgrupper);
  }),

  http.get("*/api/v1/intern/veileder/tiltakstyper", async () => {
    return HttpResponse.json(Object.values(mockTiltakstyper));
  }),

  http.get("*/api/v1/intern/tiltakstyper/stotterPameldingIModia", async () => {
    const tiltakstyperMedPamelding = [
      "ARBFORB",
      "ARBRRHDAG",
      "AVKLARAG",
      "DIGIOPPARB",
      "GRUFAGYRKE",
      "GRUPPEAMO",
      "INDOPPFAG",
      "JOBB",
      "VASV",
    ];
    return HttpResponse.json(tiltakstyperMedPamelding);
  }),

  http.get("*/api/v1/intern/veileder/tiltaksgjennomforinger", async ({ request }) => {
    const url = new URL(request.url);
    const results = getFilteredArbeidsmarkedstiltak(url);
    return HttpResponse.json(results);
  }),

  http.get<{ id: string }>(
    "*/api/v1/intern/veileder/tiltaksgjennomforinger/:id",
    async ({ params }) => {
      const { id } = params;
      const gjennomforing = findArbeidsmarkedstiltak(id);
      return HttpResponse.json(gjennomforing);
    },
  ),

  http.get("*/api/v1/intern/veileder/nav/tiltaksgjennomforinger", async ({ request }) => {
    const url = new URL(request.url);
    const results = getFilteredArbeidsmarkedstiltak(url);
    return HttpResponse.json(results);
  }),

  http.get<{ id: string }>(
    "*/api/v1/intern/veileder/nav/tiltaksgjennomforinger/:id",
    async ({ params }) => {
      const { id } = params;

      const gjennomforing = findArbeidsmarkedstiltak(id);
      if (gjennomforing) {
        gjennomforing.arrangor = undefined;
        gjennomforing.kontaktinfo = {
          tiltaksansvarlige: [],
          varsler: [KontaktinfoVarsel.IKKE_TILGANG_TIL_KONTAKTINFO],
        };
      }

      return HttpResponse.json(gjennomforing);
    },
  ),

  http.get("*/api/v1/intern/veileder/preview/tiltaksgjennomforinger", async ({ request }) => {
    const url = new URL(request.url);
    const results = getFilteredArbeidsmarkedstiltak(url);
    return HttpResponse.json(results);
  }),

  http.get<{ id: string }>(
    "*/api/v1/intern/veileder/preview/tiltaksgjennomforinger/:id",
    async ({ params }) => {
      const { id } = params;
      const gjennomforing = findArbeidsmarkedstiltak(id);
      return HttpResponse.json(gjennomforing);
    },
  ),
];

function getFilteredArbeidsmarkedstiltak(url: URL) {
  const innsatsgruppe = url.searchParams.get("innsatsgruppe") as Innsatsgruppe;
  const search = url.searchParams.get("search") ?? "";
  const tiltakstyper = url.searchParams.getAll("tiltakstyper");

  return mockTiltaksgjennomforinger
    .filter((gj) => filtrerFritekst(gj, search))
    .filter((gj) => filtrerInnsatsgruppe(gj, innsatsgruppe))
    .filter((gj) => filtrerTiltakstyper(gj, tiltakstyper));
}

function findArbeidsmarkedstiltak(id: string) {
  return mockTiltaksgjennomforinger.find((gj) => gj.sanityId === id || gj.id === id);
}

function filtrerFritekst(gjennomforing: VeilederflateTiltaksgjennomforing, sok: string): boolean {
  return sok === "" || gjennomforing.navn.toLocaleLowerCase().includes(sok.toLocaleLowerCase());
}

function filtrerInnsatsgruppe(
  gjennomforing: VeilederflateTiltaksgjennomforing,
  innsatsgruppe?: Innsatsgruppe,
): boolean {
  if (!innsatsgruppe || !gjennomforing.tiltakstype.innsatsgrupper) {
    return true;
  }

  return gjennomforing.tiltakstype.innsatsgrupper.includes(innsatsgruppe);
}

function filtrerTiltakstyper(
  gjennomforing: VeilederflateTiltaksgjennomforing,
  tiltakstyper: string[],
): boolean {
  return tiltakstyper.length === 0 || tiltakstyper.includes(gjennomforing.tiltakstype.sanityId);
}
