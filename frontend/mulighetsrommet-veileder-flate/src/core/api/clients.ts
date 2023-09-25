import { MulighetsrommetClient, OpenAPI } from "mulighetsrommet-api-client";
import { headers, toRecord } from "./headers";

OpenAPI.HEADERS = toRecord(headers);

OpenAPI.BASE = String(import.meta.env.VITE_MULIGHETSROMMET_API_BASE ?? "");

export const mulighetsrommetClient = new MulighetsrommetClient(OpenAPI);
