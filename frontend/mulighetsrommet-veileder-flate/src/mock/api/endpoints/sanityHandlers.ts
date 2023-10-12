import { DefaultBodyType, PathParams, rest } from "msw";
import {
  GetRelevanteTiltaksgjennomforingerForBrukerRequest,
  GetTiltaksgjennomforingForBrukerRequest,
  Innsatsgruppe,
  VeilederflateInnsatsgruppe,
  VeilederflateTiltaksgjennomforing,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";
import { mockInnsatsgrupper } from "../../fixtures/mockInnsatsgrupper";
import { mockTiltaksgjennomforinger } from "../../fixtures/mockTiltaksgjennomforinger";
import { mockTiltakstyper } from "../../fixtures/mockTiltakstyper";
import { ok } from "../responses";

export const sanityHandlers = [
  rest.get<DefaultBodyType, PathParams, VeilederflateInnsatsgruppe[]>(
    "*/api/v1/internal/sanity/innsatsgrupper",
    async () => {
      return ok(mockInnsatsgrupper);
    },
  ),

  rest.get<DefaultBodyType, PathParams, VeilederflateTiltakstype[]>(
    "*/api/v1/internal/sanity/tiltakstyper",
    async () => {
      return ok(Object.values(mockTiltakstyper));
    },
  ),

  rest.post<DefaultBodyType, PathParams, any>(
    "*/api/v1/internal/sanity/tiltaksgjennomforinger",
    async (req) => {
      const {
        innsatsgruppe,
        search = "",
        tiltakstypeIds = [],
      } = await req.json<GetRelevanteTiltaksgjennomforingerForBrukerRequest>();

      const results = mockTiltaksgjennomforinger
        .filter((gj) => filtrerFritekst(gj, search))
        .filter((gj) => filtrerInnsatsgruppe(gj, innsatsgruppe))
        .filter((gj) => filtrerTiltakstyper(gj, tiltakstypeIds));

      return ok(results);
    },
  ),

  rest.post<DefaultBodyType, PathParams, any>(
    "*/api/v1/internal/sanity/tiltaksgjennomforing",
    async (req) => {
      const { id } = await req.json<GetTiltaksgjennomforingForBrukerRequest>();
      const gjennomforing = mockTiltaksgjennomforinger.find((gj) => gj.sanityId === id);
      return ok(gjennomforing);
    },
  ),

  rest.get<DefaultBodyType, PathParams, any>(
    "*/api/v1/internal/sanity/tiltaksgjennomforing/preview/:id",
    async (req) => {
      const id = req.params.id;
      const gjennomforing = mockTiltaksgjennomforinger.find((gj) => gj.sanityId === id);
      return ok(gjennomforing);
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
