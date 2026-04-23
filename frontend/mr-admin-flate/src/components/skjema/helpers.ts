import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { FieldValues, Path, UseFormReturn } from "react-hook-form";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";

export function applyValidationErrors<T extends FieldValues>(
  form: UseFormReturn<T>,
  error: ValidationError,
  mapFieldName?: (name: string) => string,
) {
  error.errors.forEach((e) => {
    const rawName = jsonPointerToFieldPath(e.pointer);
    const name = (mapFieldName ? mapFieldName(rawName) : rawName) as Path<T>;
    form.setError(name, { type: "custom", message: e.detail });
  });
}

export function fp(...parts: (string | number)[]): string {
  return [...parts].filter((part) => part !== "").join(".");
}
