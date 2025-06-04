import { FieldError } from "api-client";

export function errorAt(pointer: string, errors: FieldError[] | undefined): string | undefined {
  return errors?.find((error) => error.pointer === pointer)?.detail;
}
