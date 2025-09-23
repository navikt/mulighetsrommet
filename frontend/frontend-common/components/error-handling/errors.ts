type ProblemDetail = {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  [key: string]: unknown | string | number | undefined;
};

export function isProblemDetail(error: any): error is ProblemDetail {
  return "status" in error && "detail" in error && "type" in error && "title" in error;
}

export function resolveErrorMessage(error: any): string {
  if (isProblemDetail(error)) {
    return error.detail;
  }
  return "Det skjedde en uventet feil";
}
