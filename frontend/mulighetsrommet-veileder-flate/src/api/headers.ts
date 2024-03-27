import { APPLICATION_NAME } from "@/constants";

export const headers = new Headers();

headers.append("Accept", "application/json");
headers.append("Nav-Consumer-Id", APPLICATION_NAME);

if (import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN) {
  headers.append("Authorization", `Bearer ${import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN}`);
}

export function toRecord(headers: Headers) {
  const record: Record<string, string> = {};

  headers.forEach((value, key) => {
    record[key] = value;
  });

  return record;
}
