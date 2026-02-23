import { describe, expect, it } from "vitest";
import { z } from "zod";
import { createGracefulParser } from "./filter-validator";

describe("createGracefulParser", () => {
  const TestSchema = z.object({
    name: z.string(),
    count: z.number(),
    tags: z.array(z.string()),
    object: z.object({
      string: z.string(),
    }),
    enabled: z.boolean(),
  });

  type TestType = z.infer<typeof TestSchema>;

  const defaults: TestType = {
    name: "default",
    count: 0,
    tags: [],
    object: { string: "string" },
    enabled: false,
  };

  const parser = createGracefulParser(TestSchema, defaults);

  it("returns defaults for null input", () => {
    expect(parser(null)).toEqual(defaults);
  });

  it("returns defaults for undefined input", () => {
    expect(parser(undefined)).toEqual(defaults);
  });

  it("returns defaults for non-object input", () => {
    expect(parser("string")).toEqual(defaults);
    expect(parser(123)).toEqual(defaults);
    expect(parser(true)).toEqual(defaults);
  });

  it("returns parsed data when input is fully valid", () => {
    const validInput = {
      name: "test",
      count: 42,
      tags: ["a", "b"],
      object: { string: "frank" },
      enabled: true,
    };
    expect(parser(validInput)).toEqual(validInput);
  });

  it("uses defaults for missing fields", () => {
    const partialInput = {
      name: "test",
      count: 5,
    };
    expect(parser(partialInput)).toEqual({
      name: "test",
      count: 5,
      tags: [],
      object: { string: "string" },
      enabled: false,
    });
  });

  it("uses defaults for invalid fields while keeping valid ones", () => {
    const mixedInput = {
      name: "valid",
      count: "not a number",
      tags: ["valid", "tags"],
      object: "string",
      enabled: "not a boolean",
    };
    expect(parser(mixedInput)).toEqual({
      name: "valid",
      count: 0,
      tags: ["valid", "tags"],
      object: { string: "string" },
      enabled: false,
    });
  });

  it("uses default for array field with invalid items", () => {
    const invalidArrayInput = {
      name: "test",
      count: 1,
      tags: [1, 2, 3],
      enabled: true,
    };
    expect(parser(invalidArrayInput)).toEqual({
      name: "test",
      count: 1,
      tags: [],
      object: { string: "string" },
      enabled: true,
    });
  });

  it("handles empty object input", () => {
    expect(parser({})).toEqual(defaults);
  });

  it("ignores extra fields not in schema", () => {
    const inputWithExtra = {
      name: "test",
      count: 10,
      tags: [],
      enabled: true,
      extraField: "should be ignored",
    };
    expect(parser(inputWithExtra)).toEqual({
      name: "test",
      count: 10,
      tags: [],
      object: { string: "string" },
      enabled: true,
    });
  });
});
