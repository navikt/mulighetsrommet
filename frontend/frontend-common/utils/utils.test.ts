import { describe, test, expect } from "vitest";
import { compare, jsonPointerToFieldPath } from "./utils";

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

describe("compare", () => {
  test("both null returns 0", () => {
    expect(compare(null, null)).toBe(0);
  });

  test("both undefined returns 0", () => {
    expect(compare(undefined, undefined)).toBe(0);
  });

  test("first argument null sorts last", () => {
    expect(compare(null, 1)).toBe(1);
  });

  test("second argument null sorts last", () => {
    expect(compare(1, null)).toBe(-1);
  });

  test("first argument undefined sorts last", () => {
    expect(compare(undefined, 1)).toBe(1);
  });

  test("second argument undefined sorts last", () => {
    expect(compare(1, undefined)).toBe(-1);
  });

  test("numbers: smaller values sorts first", () => {
    expect(compare(3, 5)).toBe(-2);
    expect(compare(5, 3)).toBe(2);
    expect(compare(4, 4)).toBe(0);
  });

  test("strings: smaller string sorts first", () => {
    expect(compare("a", "b")).toBe(-1);
    expect(compare("b", "a")).toBe(1);
    expect(compare("a", "a")).toBe(0);
  });

  test("dates: earlier date sorts first", () => {
    const earlier = new Date("2024-01-01");
    const later = new Date("2024-06-01");
    expect(compare(earlier, later)).toBe(-1);
    expect(compare(later, earlier)).toBe(1);
    expect(compare(earlier, earlier)).toBe(0);
  });
});
