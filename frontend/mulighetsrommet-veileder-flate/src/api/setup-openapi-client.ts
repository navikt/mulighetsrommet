import { OpenAPI } from "mulighetsrommet-api-client";
import { v4 as uuidv4 } from "uuid";
import { APPLICATION_NAME } from "@/constants";

export interface OpenAPIConfig {
  base: string;
  authToken?: string;
}

export function setupOpenAPIClient(config: OpenAPIConfig) {
  OpenAPI.BASE = config.base;

  OpenAPI.HEADERS = async () => {
    const headers: Record<string, string> = {};

    headers["Accept"] = "application/json";
    headers["Nav-Call-Id"] = uuidv4();
    headers["Nav-Consumer-Id"] = APPLICATION_NAME;

    if (config.authToken) {
      headers["Authorization"] = `Bearer ${config.authToken}`;
    }

    return headers;
  };
}
