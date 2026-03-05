import type { FieldPath, UseFormSetError } from "react-hook-form";

export function setBackendErrors<T extends Record<string, unknown>>(
  setError: UseFormSetError<T>,
  errors: Record<string, string | string[]>,
) {
  Object.entries(errors).forEach(([field, message]) => {
    const errorMessage = Array.isArray(message) ? message.join(", ") : message;
    setError(field as FieldPath<T>, {
      type: "backend",
      message: errorMessage,
    });
  });
}
