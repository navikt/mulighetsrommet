import { http, HttpResponse, PathParams } from "msw";
import {
  GetTiltaksgjennomforingerRequest,
  GetTiltaksgjennomforingRequest,
  Innsatsgruppe,
  VeilederflateInnsatsgruppe,
  VeilederflateTiltaksgjennomforing,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";
import { mockInnsatsgrupper } from "../../fixtures/mockInnsatsgrupper";
import { mockTiltaksgjennomforinger } from "../../fixtures/mockTiltaksgjennomforinger";
import { mockTiltakstyper } from "../../fixtures/mockTiltakstyper";

export const tiltakHandlers = [
  http.get<PathParams, VeilederflateInnsatsgruppe[]>(
    "*/api/v1/internal/veileder/innsatsgrupper",
    async () => {
      return HttpResponse.json(mockInnsatsgrupper);
    },
  ),

  http.get<PathParams, VeilederflateTiltakstype[]>(
    "*/api/v1/internal/veileder/tiltakstyper",
    async () => {
      return HttpResponse.json(Object.values(mockTiltakstyper));
    },
  ),

  http.post<PathParams, GetTiltaksgjennomforingerRequest>(
    "*/api/v1/internal/veileder/tiltaksgjennomforinger",
    async ({ request }) => {
      const { innsatsgruppe, search = "", tiltakstypeIds = [] } = await request.json();

      const results = mockTiltaksgjennomforinger
        .filter((gj) => filtrerFritekst(gj, search))
        .filter((gj) => filtrerInnsatsgruppe(gj, innsatsgruppe))
        .filter((gj) => filtrerTiltakstyper(gj, tiltakstypeIds));

      return HttpResponse.json(results);
    },
  ),

  http.post<PathParams, GetTiltaksgjennomforingRequest>(
    "*/api/v1/internal/veileder/tiltaksgjennomforing",
    async ({ request }) => {
      const { id } = await request.json();
      const gjennomforing = mockTiltaksgjennomforinger.find(
        (gj) => gj.sanityId === id || gj.id === id,
      );
      return HttpResponse.json(gjennomforing);
    },
  ),

  http.post<PathParams>(
    "*/api/v1/internal/veileder/preview/tiltaksgjennomforing",
    async ({ request }) => {
      const body = (await request.json()) as { id: string; brukersEnheter: string[] };
      const gjennomforing = mockTiltaksgjennomforinger.find((gj) => gj.sanityId === body.id);
      return HttpResponse.json(gjennomforing);
    },
  ),
];

function filtrerFritekst(gjennomforing: VeilederflateTiltaksgjennomforing, sok: string): boolean {
  return sok === "" || gjennomforing.navn.toLocaleLowerCase().includes(sok.toLocaleLowerCase());
}

function filtrerInnsatsgruppe(
  gjennomforing: VeilederflateTiltaksgjennomforing,
  innsatsgruppe?: Innsatsgruppe,
): boolean {
  if (!gjennomforing.tiltakstype.innsatsgruppe) {
    return true;
  }

  switch (innsatsgruppe) {
    case Innsatsgruppe.STANDARD_INNSATS: {
      return gjennomforing.tiltakstype.innsatsgruppe.nokkel === innsatsgruppe;
    }
    case Innsatsgruppe.SITUASJONSBESTEMT_INNSATS: {
      return [Innsatsgruppe.STANDARD_INNSATS, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS].includes(
        gjennomforing.tiltakstype.innsatsgruppe.nokkel,
      );
    }
    case Innsatsgruppe.SPESIELT_TILPASSET_INNSATS: {
      return [
        Innsatsgruppe.STANDARD_INNSATS,
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      ].includes(gjennomforing.tiltakstype.innsatsgruppe.nokkel);
    }
    case Innsatsgruppe.VARIG_TILPASSET_INNSATS:
    default: {
      return true;
    }
  }
}

function filtrerTiltakstyper(
  gjennomforing: VeilederflateTiltaksgjennomforing,
  tiltakstyper: string[],
): boolean {
  return tiltakstyper.length === 0 || tiltakstyper.includes(gjennomforing.tiltakstype.sanityId);
}
