import { shallowEquals } from "@mr/frontend-common";
import { describe, expect, test } from "vitest";

describe("Primitive values", () => {
  test("should return true for identical numbers", () => {
    expect(shallowEquals(1, 1)).toBe(true);
  });

  test("should return true for identical strings", () => {
    expect(shallowEquals("hello", "hello")).toBe(true);
  });

  test("should return false for different numbers", () => {
    expect(shallowEquals(1, 2)).toBe(false);
  });

  test("should return false for different strings", () => {
    expect(shallowEquals("hello", "world")).toBe(false);
  });
});

describe("Objects", () => {
  test("should return true for shallow equal objects", () => {
    const obj1 = { a: 1, b: 2 };
    const obj2 = { a: 1, b: 2 };
    expect(shallowEquals(obj1, obj2)).toBe(true);
  });

  test("should return false for objects with different values", () => {
    const obj1 = { a: 1, b: 2 };
    const obj2 = { a: 1, b: 3 };
    expect(shallowEquals(obj1, obj2)).toBe(false);
  });

  test("should return false for objects with different keys", () => {
    const obj1 = { a: 1, b: 2 };
    const obj2 = { a: 1, c: 2 };
    expect(shallowEquals(obj1, obj2)).toBe(false);
  });
});

describe("Arrays", () => {
  test("should return true for shallow equal arrays", () => {
    const arr1 = [1, 2, 3];
    const arr2 = [1, 2, 3];
    expect(shallowEquals(arr1, arr2)).toBe(true);
  });

  test("should return false for arrays with different values", () => {
    const arr1 = [1, 2, 3];
    const arr2 = [1, 2, 4];
    expect(shallowEquals(arr1, arr2)).toBe(false);
  });

  test("should return false for arrays with different lengths", () => {
    const arr1 = [1, 2, 3];
    const arr2 = [1, 2];
    expect(shallowEquals(arr1, arr2)).toBe(false);
  });
});
