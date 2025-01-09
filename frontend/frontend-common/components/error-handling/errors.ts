export function resolveErrorMessage(error: any): string {
  if (typeof error.body === "string") {
    return error.body;
  } else if (typeof (error.body as any)?.description === "string") {
    return (error.body as any).description;
  } else {
    return error.message;
  }
}

export function resolveRequestId(error: any): string | undefined {
  if (typeof (error.body as any)?.requestId === "string") {
    return (error.body as any).requestId;
  }

  return undefined;
}
