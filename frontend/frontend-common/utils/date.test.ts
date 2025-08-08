import { describe, test, expect } from "vitest";
import {
  inBetweenInclusive,
  isEarlier,
  isLater,
  isLaterOrSameDay,
  yyyyMMddFormatting,
  parseDate,
  maxOf,
} from "./date";

describe("date.ts", () => {
  describe("parseDate()", () => {
    test("null to undefined", () => {
      expect(parseDate(null)).toBe(undefined);
    });
    test("undefined to undefined", () => {
      expect(parseDate(undefined)).toBe(undefined);
    });
    test("empty string to undefined", () => {
      expect(parseDate("")).toBe(undefined);
    });
    test("invalid string to undefined", () => {
      expect(parseDate("hello")).toBe(undefined);
    });
    test("invalid date-string to undefined", () => {
      expect(parseDate("29.02.2025")).toBe(undefined);
    });
    test("invalid date to undefined", () => {
      expect(parseDate(new Date(""))).toBe(undefined);
    });
    test("valid date to date", () => {
      expect(parseDate(new Date("2025-02-28"))?.toISOString()).toBe("2025-02-28T00:00:00.000Z");
    });
    test("valid date-string to date", () => {
      expect(parseDate("2025-02-28")?.toISOString()).toBe("2025-02-28T00:00:00.000Z");
    });
    test("valid norwegian date-string to date", () => {
      expect(parseDate("28.02.2025")?.toISOString()).toBe("2025-02-28T00:00:00.000Z");
    });
    test("valid norwegian date-time-string to date", () => {
      expect(parseDate("28.02.2025 10:30")?.toISOString()).toBe("2025-02-28T10:30:00.000+01:00");
    });
  });

  describe("yyyyMMddFormatting()", () => {
    test("valid dd.MM.yyyy to yyyy-MM-dd", () => {
      expect(yyyyMMddFormatting("31.12.2025")).toBe("2025-12-31");
    });
    test("invalid dd.MM.yyyy to undefined", () => {
      expect(yyyyMMddFormatting("31.02.202")).toBe(undefined);
    });
    test("valid yyyy-MM-dd to yyyy-MM-dd", () => {
      expect(yyyyMMddFormatting("2025-12-31")).toBe("2025-12-31");
    });
    test("invalid yyyy-MM-dd to undefined", () => {
      expect(yyyyMMddFormatting("2025-12-3")).toBe(undefined);
    });
    test("valid date to yyyy-MM-dd", () => {
      expect(yyyyMMddFormatting(new Date(2025, 12 - 1, 31))).toBe("2025-12-31");
    });
    test("invalid datestring to undefined", () => {
      // Javascripts Date objekt støtter ikke norskt format dd.MM.yyyy
      expect(yyyyMMddFormatting(new Date("31.12.2025"))).toBe(undefined);
    });
    test("null to undefined", () => {
      expect(yyyyMMddFormatting(null)).toBe(undefined);
    });
    test("undefined to undefined", () => {
      expect(yyyyMMddFormatting(undefined)).toBe(undefined);
    });
  });
  describe("isEarlier()", () => {
    test("false for invalid dates", () => {
      const date = new Date("");
      expect(isEarlier("", "")).toBe(false);
      expect(isEarlier("", date)).toBe(false);
      expect(isEarlier(date, "")).toBe(false);
    });
    test("false for equal dates", () => {
      const date = "2025-07-15";
      expect(isEarlier(date, date)).toBe(false);
    });
    test("false when compared is earlier", () => {
      const date = "2025-07-15";
      const compaed = "2025-07-14";
      expect(isEarlier(date, compaed)).toBe(false);
    });
    test("true when compared is later", () => {
      const date = "2025-07-15";
      const compaed = "2025-07-16";
      expect(isEarlier(date, compaed)).toBe(true);
    });
  });
  describe("isLater()", () => {
    test("false for invalid dates", () => {
      const date = new Date("");
      expect(isLater("", "")).toBe(false);
      expect(isLater("", date)).toBe(false);
      expect(isLater(date, "")).toBe(false);
    });
    test("false for equal dates", () => {
      const date = "2025-07-15";
      expect(isLater(date, date)).toBe(false);
    });
    test("false when compared is later", () => {
      const date = "2025-07-15";
      const compaed = "2025-07-16";
      expect(isLater(date, compaed)).toBe(false);
    });
    test("true when compared is earlier", () => {
      const date = "2025-07-15";
      const compaed = "2025-07-14";
      expect(isLater(date, compaed)).toBe(true);
    });
  });
  describe("isLaterOrSameDay()", () => {
    test("false for invalid dates", () => {
      const date = new Date("");
      expect(isLaterOrSameDay("", "")).toBe(false);
      expect(isLaterOrSameDay("", date)).toBe(false);
      expect(isLaterOrSameDay(date, "")).toBe(false);
    });
    test("false for equal dates", () => {
      const date = "2025-07-15";
      expect(isLaterOrSameDay(date, date)).toBe(true);
    });
    test("false when compared is later", () => {
      const date = "2025-07-15";
      const compaed = "2025-07-16";
      expect(isLaterOrSameDay(date, compaed)).toBe(false);
    });
    test("true when compared is earlier", () => {
      const date = "2025-07-15";
      const compaed = "2025-07-14";
      expect(isLaterOrSameDay(date, compaed)).toBe(true);
    });
  });
  describe("inBetweenInclusive()", () => {
    test("false for invalid dates", () => {
      expect(inBetweenInclusive("", { from: "", to: "" })).toBe(false);
    });
    test("false when date is earlier than range", () => {
      expect(inBetweenInclusive("2025-07-01", { from: "2025-07-02", to: "2025-07-04" })).toBe(false);
    });
    test("false when date is later than range", () => {
      expect(inBetweenInclusive("2025-07-05", { from: "2025-07-02", to: "2025-07-04" })).toBe(false);
    });
    test("true when date is from", () => {
      expect(inBetweenInclusive("2025-07-02", { from: "2025-07-02", to: "2025-07-04" })).toBe(true);
    });
    test("true when date is to", () => {
      expect(inBetweenInclusive("2025-07-04", { from: "2025-07-02", to: "2025-07-04" })).toBe(true);
    });
    test("true when date is between", () => {
      expect(inBetweenInclusive("2025-07-03", { from: "2025-07-02", to: "2025-07-04" })).toBe(true);
    });
    test("true when date is over new year", () => {
      expect(inBetweenInclusive("2026-01-01", { from: "2025-12-31", to: "2026-01-02" })).toBe(true);
    });
  });
  describe("maxOf()", () => {
    test("invalid dates", () => {
      expect(maxOf(["", undefined, null]).getTime()).toBe(NaN);
    });
    test("latest, earliest first", () => {
      const expected = new Date(2025, 6, 17)
      expect(maxOf([new Date(2025, 5, 17), expected]).toISOString()).toBe(expected.toISOString());
    });
    test("latest, latest first", () => {
      const expected = new Date(2025, 6, 17)
      expect(maxOf([expected, new Date(2025, 5, 17)]).toISOString()).toBe(expected.toISOString());
    });
  })
});
