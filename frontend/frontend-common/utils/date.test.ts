import { describe, test, expect } from "vitest";
import { isoDateStringFormat, parseDate } from "./date";

describe("date.ts", () => {
  describe("parseDate()", () => {
    test("null to undefined", () => {
        expect(parseDate(null), undefined)
    })
    test("undefined to undefined", () => {
        expect(parseDate(undefined), undefined)
    })
    test("empty string to undefined", () => {
        expect(parseDate(""), undefined)
    })
    test("invalid string to undefined", () => {
        expect(parseDate("hello"), undefined)
    })
    test("invalid date-string to undefined", () => {
        expect(parseDate("29.02.2024"), undefined)
    })
    test("invalid date to undefined", () => {
        expect(parseDate(new Date("")), undefined)
    })
    test("valid date to date", () => {
        expect(parseDate(new Date(2024,1,28))?.toISOString(),"2024-02-28")
    })
    test("valid date-string to date", () => {
        expect(parseDate("2024-02-28")?.toISOString(),"2024-02-28")
    })
    test("valid norwegian date-string to date", () => {
        expect(parseDate("28.02.2024")?.toISOString(),"2024-02-28")
    })
    test("valid norwegian date-time-string to date", () => {
        expect(parseDate("28.02.2024 10:30")?.toISOString(),"2024-02-28")
    })
  })

  describe("isoDateStringFormat()", () => {
    const fallback = "<fallbackDate>";

    test("valid dd.MM.yyyy to yyyy-MM-dd", () => {
      expect(isoDateStringFormat("31.12.2024", fallback)).toBe("2024-12-31");
    });
    test("invalid dd.MM.yyyy to fallback", () => {
      expect(isoDateStringFormat("31.02.202", fallback)).toBe(fallback);
    });
    test("valid yyyy-MM-dd to yyyy-MM-dd", () => {
      expect(isoDateStringFormat("2024-12-31", fallback)).toBe("2024-12-31");
    });
    test("invalid yyyy-MM-dd to fallback", () => {
      expect(isoDateStringFormat("2024-12-3", fallback)).toBe(fallback);
    });
    test("valid date to yyyy-MM-dd", () => {
      expect(isoDateStringFormat(new Date(2024, 12 - 1, 31), fallback)).toBe("2024-12-31");
    });
    test("invalid datestring to fallback", () => {
      expect(isoDateStringFormat(new Date("31.12.2024"), fallback)).toBe(fallback);
    });
    test("null to fallback", () => {
      expect(isoDateStringFormat(null, fallback)).toBe(fallback);
    });
    test("undefined to fallback", () => {
      expect(isoDateStringFormat(undefined, fallback)).toBe(fallback);
    });
  });
});
