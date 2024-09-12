import { OpenAPI } from "@mr/api-client";
import { getToken, requestTokenxOboToken, validateToken } from "@navikt/oasis";
import { redirectDocument } from "@remix-run/node";
import { v4 as uuidv4 } from "uuid";

export async function oboExchange(request: Request) {
  if (process.env.NODE_ENV !== 'production') {
    return;
  }

  const token = getToken(request);
  if (!token) {
    console.log("missing token");
    throw redirectDocument("/oauth2/login");
  }
  const validation = await validateToken(token);
  if (!validation.ok) {
    console.log("invalid token");
    throw redirectDocument("/oauth2/login");
  }
  const obo = await requestTokenxOboToken(token, `${process.env.NAIS_CLUSTER_NAME}.team-mulighetsrommet.mulighetsrommet-api`);
  if (!obo.ok) {
    console.log("obo exchange failed");
    throw redirectDocument("/oauth2/login");
  }

  setOpenApiHeaders(obo.token);
}

function setOpenApiHeaders(token: string) {
  OpenAPI.HEADERS = async () => {
    const headers: Record<string, string> = {};

    headers["Accept"] = "application/json";
    headers["Nav-Consumer-Id"] = uuidv4();

    headers["Authorization"] = `Bearer ${token}`

    return headers;
  };
}
