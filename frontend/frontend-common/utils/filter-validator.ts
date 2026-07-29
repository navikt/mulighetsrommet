import z, { ZodType } from "zod";

export function createFilterValidator<T>(schema: ZodType<T>) {
  return (values: unknown): values is T => {
    return Boolean(schema.safeParse(values).success);
  };
}

export function createGracefulParser<T extends object>(
  schema: z.ZodObject<z.ZodRawShape>,
  defaults: T,
): (input: unknown) => T {
  return (input: unknown): T => {
    if (!input || typeof input !== "object") {
      return defaults;
    }

    const result = schema.safeParse(input);
    if (result.success) {
      return result.data as T;
    }

    return mergeWithDefaults(schema, defaults, input as Record<string, unknown>);
  };
}

function mergeWithDefaults<T extends object>(
  schema: z.ZodObject<z.ZodRawShape>,
  defaults: T,
  input: Record<string, unknown>,
): T {
  const merged: Record<string, unknown> = {};

  for (const key of Object.keys(defaults)) {
    if (key in input) {
      const fieldSchema = schema.pick({ [key]: true });
      const fieldResult = fieldSchema.safeParse({ [key]: input[key] });
      merged[key] = fieldResult.success ? fieldResult.data[key] : defaults[key as keyof T];
    } else {
      merged[key] = defaults[key as keyof T];
    }
  }

  return merged as T;
}
