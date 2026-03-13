import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { FieldValues, Path, UseFormReturn } from "react-hook-form";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";

export function applyValidationErrors<T extends FieldValues>(
  form: UseFormReturn<T>,
  error: ValidationError,
) {
  error.errors.forEach((e) => {
    const name = jsonPointerToFieldPath(e.pointer) as Path<T>;
    form.setError(name, { type: "custom", message: e.detail });
  });
}
