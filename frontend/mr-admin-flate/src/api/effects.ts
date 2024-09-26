import { UseMutationResult } from "@tanstack/react-query";
import { useEffect } from "react";
import { ApiError, ValidationErrorResponse } from "@mr/api-client";

export function useHandleApiUpsertResponse<Response, Request>(
  mutation: UseMutationResult<Response, ApiError, Request>,
  onSuccess: (response: Response) => void,
  onValidationError: (response: ValidationErrorResponse) => void,
) {
  useEffect(() => {
    if (mutation.isSuccess) {
      onSuccess(mutation.data);
    } else if (mutation.isError) {
      if (isValidationError(mutation.error.body)) {
        onValidationError(mutation.error.body);
      } else {
        throw mutation.error;
      }
    }
  }, [
    mutation.isSuccess,
    mutation.isError,
    mutation.data,
    mutation.error,
    onSuccess,
    onValidationError,
  ]);
}

function isValidationError(body: unknown): body is ValidationErrorResponse {
  return (
    body !== null &&
    typeof body === "object" &&
    Object.prototype.hasOwnProperty.call(body, "errors")
  );
}
