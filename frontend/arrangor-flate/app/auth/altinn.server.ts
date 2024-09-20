import { getEnv } from "../env/envUtils";
import { requireUserId, tokenXExchange } from "./auth.server";

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
  const token = tokenXExchange(request);

  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };

  const response = await fetch(`${getBaseUrl()}/api/v1/rolle/tiltaksarrangor`, {
    method: "POST",
    body: JSON.stringify({ personident }),
    headers,
  });

  if (!response.ok) {
    // eslint-disable-next-line no-console
    console.log(
      `Klarte ikke hente tilganger for bruker. Status = ${response.status} - Error: ${response.statusText} `,
    );
    throw new Error("Klarte ikke hente tilganger for bruker");
  }

  return (await response.json()) as TilgangerResponse;
}

function getBaseUrl(): string {
  const devBase = "https://mulighetsrommet-altinn-acl.intern.dev.nav.no";
  const prodBase = "https://mulighetsrommet-altinn-acl.intern.nav.no";

  switch (getEnv()) {
    case "local":
      throw new Error("Local not supported yet");
    case "dev-gcp":
      return devBase;
    case "prod-gcp":
      return prodBase;
  }
}
