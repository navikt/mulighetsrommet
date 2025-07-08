import { FieldError, ProblemDetail, ValidationError } from "api-client";

export function errorAt(pointer: string, errors: FieldError[] | undefined): string | undefined {
  return errors?.find((error) => error.pointer === pointer)?.detail;
}

export function isValidationError(error: unknown): error is ValidationError {
  return typeof error === "object" && error !== null && "errors" in error;
}

export function problemDetailResponse(error: ProblemDetail): Response {
  return new Response(JSON.stringify(error), {
    status: error.status,
    headers: { "Content-Type": "application/json" },
  });
}
