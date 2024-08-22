import { UseMutationResult } from "@tanstack/react-query";
import { useEffect } from "react";
import { ApiError, ValidationErrorResponse } from "@mr/api-client";

export function useHandleApiUpsertResponse<Response, Request>(
  mutation: UseMutationResult<Response, ApiError, Request>,
  onSuccess: (response: Response) => void,
  onValidationError: (response: ValidationErrorResponse) => void,
) {
  useEffect(() => {
    const { isSuccess, data, isError, error } = mutation;
    if (isSuccess) {
      onSuccess(data);
    } else if (isError) {
      if (isValidationError(error.body)) {
        onValidationError(error.body);
      } else {
        throw mutation.error;
      }
    }
  }, [mutation, onSuccess, onValidationError]);
}

function isValidationError(body: unknown): body is ValidationErrorResponse {
  return (
    body !== null &&
    typeof body === "object" &&
    Object.prototype.hasOwnProperty.call(body, "errors")
  );
}
