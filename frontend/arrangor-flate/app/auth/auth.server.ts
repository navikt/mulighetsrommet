import { OpenAPI } from "@mr/api-client";
import { getToken, parseIdportenToken, requestTokenxOboToken, validateToken } from "@navikt/oasis";
import { redirectDocument } from "@remix-run/node";
import { v4 as uuidv4 } from "uuid";

const loginUrl = "/oauth2/login";

export async function oboExchange(request: Request) {
  if (process.env.NODE_ENV !== "production") {
    return;
  }

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
  const obo = await requestTokenxOboToken(
    token,
    `${process.env.NAIS_CLUSTER_NAME}:team-mulighetsrommet:mulighetsrommet-api`,
  );
  if (!obo.ok) {
    // eslint-disable-next-line no-console
    console.log("obo exchange failed", obo);
    throw redirectDocument(loginUrl);
  }

  setOpenApiHeaders(obo.token);
}

export async function tokenXExchangeAltinnAcl(request: Request) {
  if (process.env.NODE_ENV !== "production") {
    return;
  }

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
  const obo = await requestTokenxOboToken(
    token,
    `${process.env.NAIS_CLUSTER_NAME}:team-mulighetsrommet:mulighetsrommet-altinn-acl`,
  );
  if (!obo.ok) {
    // eslint-disable-next-line no-console
    console.log("obo exchange failed", obo);
    throw redirectDocument(loginUrl);
  }

  // eslint-disable-next-line no-console
  console.log("Token: ", obo.token); // TODO Fjern meg
  return obo.token;
}

async function getPersonIdent(request: Request): Promise<string | undefined> {
  const token = await getToken(request);

  if (!token) {
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

  return parsed.pid;
}

export async function requireUserId(request: Request) {
  if (process.env.NODE_ENV !== "production") return "12345678910"; // TODO Finne ut hvordan vi vil løse det uten auth ved lokal utvikling

  const userId = await getPersonIdent(request);
  if (!userId) {
    throw redirectDocument(loginUrl);
  }
  return userId;
}

function setOpenApiHeaders(token: string) {
  OpenAPI.HEADERS = async () => {
    const headers: Record<string, string> = {};

    headers["Accept"] = "application/json";
    headers["Nav-Consumer-Id"] = uuidv4();

    headers["Authorization"] = `Bearer ${token}`;

    return headers;
  };
}
