import { Issuer } from "openid-client";
import type { Client } from "openid-client";
import { logger } from "server/logger";

const config = {
  discoveryUrl: process.env.TOKEN_X_WELL_KNOWN_URL!,
  clientId: process.env.TOKEN_X_CLIENT_ID!,
  privateJwk: process.env.TOKEN_X_PRIVATE_JWK!,
  tokenEndpoint: process.env.TOKEN_X_TOKEN_ENDPOINT!,
};

class TokenClient {
  private client?: Client;

  hent = async (): Promise<Client> => {
    if (this.client === undefined) {
      const issuerClient = await this.configure();

      this.client = issuerClient;
      return issuerClient;
    }

    return this.client;
  };

  configure = async (): Promise<Client> => {
    logger.info(
      `Konfigurerer TokenX med clientId ${config.clientId} og discoveryUrl ${config.discoveryUrl} ...`,
    );

    const issuer = await this.discoverIssuer();

    const jwk = JSON.parse(config.privateJwk);
    const issuerClient = new issuer.Client(
      {
        client_id: config.clientId,
        token_endpoint_auth_method: "private_key_jwt",
      },
      {
        keys: [jwk],
      },
    );

    logger.info("TokenX er konfigurert!");

    return issuerClient;
  };

  veksleToken = async (accessToken: string, scope: string) => {
    const now = Math.floor(Date.now() / 1000);
    const tokenXClient = await this.hent();

    return tokenXClient.grant(
      {
        grant_type: "urn:ietf:params:oauth:grant-type:token-exchange",
        client_assertion_type: "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
        subject_token_type: "urn:ietf:params:oauth:token-type:jwt",
        audience: scope,
        subject_token: accessToken,
      },
      {
        clientAssertionPayload: {
          nbf: now,
          aud: [config.tokenEndpoint],
        },
      },
    );
  };

  discoverIssuer = async () => {
    if (config.discoveryUrl) {
      return await Issuer.discover(config.discoveryUrl);
    } else {
      throw Error(`Miljøvariabelen "TOKEN_X_WELL_KNOWN_URL" må være definert`);
    }
  };
}

export default TokenClient;
