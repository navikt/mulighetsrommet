import { requireUserId, tokenXExchangeAltinnAcl } from "./auth.server";

interface TilgangerResponse {
  roller: TiltaksarrangorRoller[];
}

interface TiltaksarrangorRoller {
  organisasjonsnummer: string;
  roller: Roller[];
}

type Roller = "TILTAK_ARRANGOR_REFUSJON";

export async function getTilganger(request: Request): Promise<TilgangerResponse> {
  const personident = requireUserId(request);
  const token = tokenXExchangeAltinnAcl(request);

  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };

  const response = await fetch(`http://mulighetsrommet-altinn-acl/api/v1/rolle/tiltaksarrangor`, {
    method: "POST",
    body: JSON.stringify({ personident }),
    headers,
  });

  if (!response.ok) {
    // eslint-disable-next-line no-console
    console.log(
      `Klarte ikke hente tilganger for bruker. Status = ${response.status} - Error: ${response.statusText} - ${JSON.stringify(response, null, 2)}`,
    );
    throw new Error("Klarte ikke hente tilganger for bruker");
  }

  return (await response.json()) as TilgangerResponse;
}
