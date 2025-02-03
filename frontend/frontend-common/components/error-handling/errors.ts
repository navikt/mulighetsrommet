import { ProblemDetail } from "@mr/api-client-v2";

export function isProblemDetail(error: any): error is ProblemDetail {
  return 'status' in error && 'detail' in error && 'type' in error && 'title' in error;
}

export function resolveErrorMessage(error: any): string {
  if (isProblemDetail(error)) {
    return error.detail;
  }
  return "foo";
}

