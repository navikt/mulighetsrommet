import { FieldError } from "@mr/api-client-v2";

export function getOrThrowError(formData: FormData, property: string) {
  const data = formData.get(property);
  if (!data) {
    throw new Error(`Mangler ${property}`);
  }
  return data;
}

export function getOrError(
  formData: FormData,
  property: string,
  detail: string,
): { error: FieldError; data?: undefined } | { error?: undefined; data: FormDataEntryValue } {
  const data = formData.get(property);
  if (!data) {
    return { error: { pointer: `/${property}`, detail } };
  }
  return { data };
}
