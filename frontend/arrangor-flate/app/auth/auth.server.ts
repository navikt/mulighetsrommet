import { OpenAPI } from "@mr/api-client";
import { getToken, parseIdportenToken, requestTokenxOboToken, validateToken } from "@navikt/oasis";
import { redirectDocument } from "react-router";
import { v4 as uuidv4 } from "uuid";
import { hentMiljø, Miljø } from "../services/miljø";

const loginUrl = "/oauth2/login";

export async function oboExchange(request: Request, audience: string) {
  const token = getToken(request);

  if (!token) {
    // eslint-disable-next-line no-console
    console.log("missing token");
    throw redirectDocument(loginUrl);
  }

  const validation = await validateToken(token);
  if (!validation.ok) {
    // eslint-disable-next-line no-console
    console.log("invalid token");
    throw redirectDocument(loginUrl);
  }

  const obo = await requestTokenxOboToken(token, audience);
  if (!obo.ok) {
    // eslint-disable-next-line no-console
    console.log("obo exchange failed", obo);
    throw redirectDocument(loginUrl);
  }

  return obo.token;
}

export async function setupOpenApi(request: Request) {
  const token =
    hentMiljø() === Miljø.Lokalt
      ? process.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN
      : await oboExchange(
          request,
          `${process.env.NAIS_CLUSTER_NAME}:team-mulighetsrommet:mulighetsrommet-api`,
        );

  setOpenApiHeaders(token);
}

export async function checkValidToken(request: Request) {
  if (hentMiljø() === Miljø.Lokalt) return;
  const token = getToken(request);

  if (!token) {
    // eslint-disable-next-line no-console
    console.error("No token present");
    throw redirectDocument(loginUrl);
  }

  const validation = await validateToken(token);

  if (!validation.ok) {
    // eslint-disable-next-line no-console
    console.error("Token not valid: ", validation);
    throw redirectDocument(loginUrl);
  }

  const parsed = parseIdportenToken(token);

  if (!parsed.ok) {
    // eslint-disable-next-line no-console
    console.log("Could not parse token for idPorten: ", parsed);
    throw redirectDocument(loginUrl);
  }
}

function setOpenApiHeaders(token?: string) {
  OpenAPI.HEADERS = async () => {
    const headers: Record<string, string> = {};

    headers["Accept"] = "application/json";
    headers["Nav-Consumer-Id"] = uuidv4();

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    return headers;
  };
}
