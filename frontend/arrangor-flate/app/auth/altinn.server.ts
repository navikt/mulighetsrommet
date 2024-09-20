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
  const token = await tokenXExchangeAltinnAcl(request);

  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };

  const payload = {
    method: "POST",
    body: JSON.stringify({ personident }),
    headers: {
      ...headers,
    },
  };
  // eslint-disable-next-line no-console
  console.log("Payload to altinn-acl: ", JSON.stringify(payload, null, 2)); // TODO Fjern meg
  const response = await fetch(
    `http://mulighetsrommet-altinn-acl/api/v1/rolle/tiltaksarrangor`,
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
