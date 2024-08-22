import { ApiError } from "@mr/api-client";

export function resolveErrorMessage(error: ApiError): string {
  if (typeof error.body === "string") {
    return error.body;
  } else if (typeof (error.body as any)?.description === "string") {
    return (error.body as any).description;
  } else {
    return error.message;
  }
}

export function resolveRequestId(error: ApiError): string | undefined {
  if (typeof (error.body as any)?.requestId === "string") {
    return (error.body as any).requestId;
  }

  return undefined;
}
