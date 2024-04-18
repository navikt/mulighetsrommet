import { ApiError } from "mulighetsrommet-api-client";

export function resolveErrorMessage(error: ApiError): string {
  if (typeof error.body === "string") {
    return error.body;
  } else if (typeof error.body?.description === "string") {
    return error.body.description;
  } else {
    return error.message;
  }
}

export function resolveRequestId(error: ApiError): string | undefined {
  if (typeof error.body?.requestId === "string") {
    return error.body.requestId;
  }

  return undefined;
}
