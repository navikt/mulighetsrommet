import { describe, test, expect } from "vitest";
import { jsonPointerToFieldPath } from "./utils";

describe("Json pointer", () => {
  test("json pointer konvertering - simple case", () => {
    expect(jsonPointerToFieldPath("/foo/0/bar")).toBe("foo.0.bar");
  });

  test("root path", () => {
    expect(jsonPointerToFieldPath("/")).toBe("");
  });

  test("single level", () => {
    expect(jsonPointerToFieldPath("/foo")).toBe("foo");
  });

  test("numeric key in path", () => {
    expect(jsonPointerToFieldPath("/0/foo")).toBe("0.foo");
  });

  test("nested path with multiple levels", () => {
    expect(jsonPointerToFieldPath("/foo/bar/baz")).toBe("foo.bar.baz");
  });

  test("trailing slash", () => {
    expect(jsonPointerToFieldPath("/foo/bar/")).toBe("foo.bar");
  });

  test("leading and trailing slashes", () => {
    expect(jsonPointerToFieldPath("//foo/bar//")).toBe("foo.bar");
  });

  test("JSON pointer escaping (~0 -> ~, ~1 -> /)", () => {
    expect(jsonPointerToFieldPath("/foo~0bar/baz~1qux")).toBe("foo~bar.baz/qux");
  });

  test("double slashes", () => {
    expect(jsonPointerToFieldPath("/foo//bar")).toBe("foo.bar");
  });

  test("empty input", () => {
    expect(jsonPointerToFieldPath("")).toBe("");
  });

  test("only slash", () => {
    expect(jsonPointerToFieldPath("/")).toBe("");
  });
});
