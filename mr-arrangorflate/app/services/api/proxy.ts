import { logger } from "../../../server/logger";
import { hentMiljø, Miljø } from "../miljø";
import TokenClient from "./TokenClient.server";

export const client = new TokenClient();

const naisCluster = process.env.NAIS_CLUSTER_NAME;
const apiUrl = hentMiljø() !== Miljø.Lokalt ? process.env.API_URL : "http://localhost:3000";

export const apiConfig = {
    scope: `${naisCluster}:toi:presenterte-kandidater-api`,
    url: apiUrl,
};

export const proxyTilApi = async (request: Request, url: string, method = "GET", body?: object) => {
    let maybeAuthorizationHeader: MaybeAuthorizationHeader;
    try {
        maybeAuthorizationHeader = await opprettAuthorizationHeader(request, apiConfig.scope);
    } catch (e) {
        logger.warn("Klarte ikke å opprette authorization header:", e);
        return new Response("", { status: 401 });
    }

    let headers;
    if (maybeAuthorizationHeader.isPresent) {
        headers = maybeAuthorizationHeader.authorizationHeader;
    } else {
        logger.info(maybeAuthorizationHeader.cause);
    }

    const options: RequestInit = {
        method,
        headers,
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    return await fetch(`${apiConfig.url}${url}`, options);
};

const getAccessToken = (request: Request) => {
    return request.headers.get("authorization")?.replace("Bearer", "");
};

export type MaybeAuthorizationHeader =
    | { isPresent: true; authorizationHeader: { authorization: string } }
    | { isPresent: false; cause: string };
export const opprettAuthorizationHeader = async (
    request: Request,
    scope: string
): Promise<MaybeAuthorizationHeader> => {
    if (process.env.NODE_ENV === "development") {
        return {
            isPresent: true,
            authorizationHeader: {
                authorization: ``,
            },
        };
    }

    const accessToken = getAccessToken(request);
    if (!accessToken) {
        return {
            isPresent: false,
            cause: "Request mangler access token, brukeren er sannsynligvis ikke logget inn",
        };
    }

    const exchangeToken = await client.veksleToken(accessToken, scope);
    return {
        isPresent: true,
        authorizationHeader: {
            authorization: `Bearer ${exchangeToken.access_token}`,
        },
    };
};
