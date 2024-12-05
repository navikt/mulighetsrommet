export type FormError = {
  name: string;
  message: string;
};

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
  message: string,
): { error: FormError; data?: undefined } | { error?: undefined; data: FormDataEntryValue } {
  const data = formData.get(property);
  if (!data) {
    return { error: { name: property, message } };
  }
  return { data };
}
