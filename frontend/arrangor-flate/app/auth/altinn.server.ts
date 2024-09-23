import { Miljø, hentMiljø } from "~/services/miljø";
import { oboExchange, requirePersonIdent } from "./auth.server";

interface TilgangerResponse {
  roller: TiltaksarrangorRoller[];
}

interface TiltaksarrangorRoller {
  organisasjonsnummer: string;
  roller: Roller[];
}

type Roller = "TILTAK_ARRANGOR_REFUSJON";

export async function getTilganger(request: Request): Promise<TilgangerResponse> {
  if (hentMiljø() === Miljø.Lokalt) {
    return Promise.resolve({
      roller: [
        {
          organisasjonsnummer: "974543036",
          roller: ["TILTAK_ARRANGOR_REFUSJON"],
        },
      ],
    });
  }

  const [personident, token] = await Promise.all([
    requirePersonIdent(request),
    oboExchange(
      request,
      `${process.env.NAIS_CLUSTER_NAME}:team-mulighetsrommet:mulighetsrommet-altinn-acl`,
    ),
  ]);

  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };

  const payload = {
    method: "POST",
    body: JSON.stringify({ personident: personident }),
    headers: {
      ...headers,
    },
  };

  // TODO Sette opp openAPi-client med "hardkodet" openapi.yaml
  const response = await fetch(
    "http://mulighetsrommet-altinn-acl/api/v1/rolle/tiltaksarrangor",
    payload,
  );

  if (!response.ok) {
    // eslint-disable-next-line no-console
    console.log(
      `Klarte ikke hente tilganger for bruker. Status = ${response.status} - Error: ${response.statusText} - ${JSON.stringify(response, null, 2)}`,
    );
    throw new Error("Klarte ikke hente tilganger for bruker");
  }

  return (await response.json()) as TilgangerResponse;
}
