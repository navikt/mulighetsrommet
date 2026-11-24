type ProblemDetail = {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  [key: string]: unknown | string | number | undefined;
};

export function isProblemDetail(error: unknown): error is ProblemDetail {
  return (
    typeof error === "object" &&
    error !== null &&
    "status" in error &&
    "detail" in error &&
    "type" in error &&
    "title" in error
  );
}

export function getProblemDetailTraceId(error: unknown): string | undefined {
  if (!isProblemDetail(error)) {
    return undefined;
  }

  const traceId = error.traceId;
  return typeof traceId === "string" && traceId.trim().length > 0 ? traceId : undefined;
}

export function resolveErrorMessage(error: unknown): string {
  if (isProblemDetail(error)) {
    return error.detail;
  }
  return "Det skjedde en uventet feil";
}
