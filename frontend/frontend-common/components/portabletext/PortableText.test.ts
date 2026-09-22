import { describe, expect, it } from "vitest";
import { utledAlertVariant } from "./alertVariant";

describe("utledAlertVariant", () => {
  it("henter ut variant fra Sanity-array (unngår Alert e.split-krasj)", () => {
    expect(utledAlertVariant(["warning"] as never)).toBe("warning");
    expect(utledAlertVariant(["info"] as never)).toBe("info");
    expect(utledAlertVariant(["error"] as never)).toBe("error");
  });

  it("støtter variant som ren streng", () => {
    expect(utledAlertVariant("warning")).toBe("warning");
  });

  it("faller tilbake til info for tom eller ugyldig variant", () => {
    expect(utledAlertVariant([] as never)).toBe("info");
    expect(utledAlertVariant(["bogus"] as never)).toBe("info");
    expect(utledAlertVariant(undefined as never)).toBe("info");
  });
});
