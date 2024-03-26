import { MulighetsrommetClient, OpenAPI } from "mulighetsrommet-api-client";
import { headers, toRecord } from "./headers";
import { v4 as uuidv4 } from "uuid";

OpenAPI.HEADERS = async () => {
  const record = toRecord(headers);
  record["Nav-Call-Id"] = uuidv4();
  return record;
};

OpenAPI.BASE = String(import.meta.env.VITE_MULIGHETSROMMET_API_BASE ?? "");

export const mulighetsrommetClient = new MulighetsrommetClient(OpenAPI);
