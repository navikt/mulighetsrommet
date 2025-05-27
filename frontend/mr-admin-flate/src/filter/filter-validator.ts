import { ZodType } from "zod";

export function createFilterValidator<T>(schema: ZodType<T>) {
  return (values: unknown): values is T => {
    return Boolean(schema.safeParse(values).success);
  };
}
