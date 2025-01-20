import { http, HttpResponse } from "msw";
import { Innsatsgruppe, VeilederflateTiltak } from "@mr/api-client-v2";
import { mockInnsatsgrupper } from "@/mock/fixtures/mockInnsatsgrupper";
import { mockTiltakstyper } from "@/mock/fixtures/mockTiltakstyper";
import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";
import { mockGjennomforinger } from "@/mock/fixtures/mockGjennomforinger";

export const tiltakHandlers = [
  http.get("*/api/v1/intern/veileder/innsatsgrupper", async () => {
    return HttpResponse.json(mockInnsatsgrupper);
  }),

  http.get("*/api/v1/intern/veileder/tiltakstyper", async () => {
    return HttpResponse.json(Object.values(mockTiltakstyper));
  }),

  http.get("*/api/v1/intern/tiltakstyper/stotterPameldingIModia", async () => {
    const tiltakstyperMedPamelding = [
      "AVKLARING",
      "OPPFOLGING",
      "GRUPPE_ARBEIDSMARKEDSOPPLAERING",
      "JOBBKLUBB",
      "DIGITALT_OPPFOLGINGSTILTAK",
      "ARBEIDSFORBEREDENDE_TRENING",
      "GRUPPE_FAG_OG_YRKESOPPLAERING",
      "ARBEIDSRETTET_REHABILITERING",
      "VARIG_TILRETTELAGT_ARBEID_SKJERMET",
    ];
    return HttpResponse.json(tiltakstyperMedPamelding);
  }),

  http.get("*/api/v1/intern/veileder/gjennomforinger", async ({ request }) => {
    const url = new URL(request.url);
    const results = getFilteredArbeidsmarkedstiltak(url);
    return HttpResponse.json(results);
  }),

  http.get<{ id: string }>("*/api/v1/intern/veileder/gjennomforinger/:id", async ({ params }) => {
    const { id } = params;
    const gjennomforing = findArbeidsmarkedstiltak(id);
    return HttpResponse.json(gjennomforing);
  }),

  http.get("*/api/v1/intern/veileder/nav/gjennomforinger", async ({ request }) => {
    const url = new URL(request.url);
    const results = getFilteredArbeidsmarkedstiltak(url);
    return HttpResponse.json(results);
  }),

  http.get<{ id: string }>(
    "*/api/v1/intern/veileder/nav/gjennomforinger/:id",
    async ({ params }) => {
      const { id } = params;

      const gjennomforing = findArbeidsmarkedstiltak(id);

      return HttpResponse.json(gjennomforing);
    },
  ),

  http.get("*/api/v1/intern/veileder/preview/gjennomforinger", async ({ request }) => {
    const url = new URL(request.url);
    const results = getFilteredArbeidsmarkedstiltak(url);
    return HttpResponse.json(results);
  }),

  http.get<{ id: string }>(
    "*/api/v1/intern/veileder/preview/gjennomforinger/:id",
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

  return mockGjennomforinger
    .filter((gj) => filtrerFritekst(gj, search))
    .filter((gj) => filtrerInnsatsgruppe(gj, innsatsgruppe))
    .filter((gj) => filtrerTiltakstyper(gj, tiltakstyper));
}

function findArbeidsmarkedstiltak(id: string) {
  return mockGjennomforinger.find((gj) => (isTiltakGruppe(gj) ? gj.id === id : gj.sanityId === id));
}

function filtrerFritekst(gjennomforing: VeilederflateTiltak, sok: string): boolean {
  return sok === "" || gjennomforing.navn.toLocaleLowerCase().includes(sok.toLocaleLowerCase());
}

function filtrerInnsatsgruppe(
  gjennomforing: VeilederflateTiltak,
  innsatsgruppe?: Innsatsgruppe,
): boolean {
  if (!innsatsgruppe || !gjennomforing.tiltakstype.innsatsgrupper) {
    return true;
  }

  return gjennomforing.tiltakstype.innsatsgrupper.includes(innsatsgruppe);
}

function filtrerTiltakstyper(gjennomforing: VeilederflateTiltak, tiltakstyper: string[]): boolean {
  return tiltakstyper.length === 0 || tiltakstyper.includes(gjennomforing.tiltakstype.sanityId);
}
