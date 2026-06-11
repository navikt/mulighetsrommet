import { Periode } from "@arrangor-utbetalinger/api-client";
import { describe, expect, test } from "vitest";
import { filtrerOverlappendePerioder, overlapperPeriode } from "./periode-filtrering";

const valgtPeriode: Periode = { start: "2026-05-01", slutt: "2026-06-01" };

describe("periode filtrering", () => {
  describe("overlapperPeriode", () => {
    test("periode helt innenfor overlapper", () => {
      expect(overlapperPeriode(valgtPeriode, { start: "2026-05-04", slutt: "2026-05-10" })).toBe(
        true,
      );
    });

    test("periode som omslutter valgtPeriode overlapper", () => {
      expect(overlapperPeriode(valgtPeriode, { start: "2026-04-01", slutt: "2026-07-01" })).toBe(
        true,
      );
    });

    test("periode som starter på valgtPeriode.start overlapper", () => {
      expect(overlapperPeriode(valgtPeriode, { start: "2026-05-01", slutt: "2026-05-15" })).toBe(
        true,
      );
    });

    test("periode som slutter på valgtPeriode.slutt overlapper", () => {
      expect(overlapperPeriode(valgtPeriode, { start: "2026-05-10", slutt: "2026-06-01" })).toBe(
        true,
      );
    });

    test("periode som starter på valgtPeriode.slutt overlapper ikke", () => {
      expect(overlapperPeriode(valgtPeriode, { start: "2026-06-01", slutt: "2026-06-30" })).toBe(
        false,
      );
    });

    test("periode som slutter på valgtPeriode.start overlapper ikke", () => {
      expect(overlapperPeriode(valgtPeriode, { start: "2026-04-01", slutt: "2026-05-01" })).toBe(
        false,
      );
    });

    test("periode helt utenfor etter valgtPeriode overlapper ikke", () => {
      expect(overlapperPeriode(valgtPeriode, { start: "2026-07-01", slutt: "2026-08-01" })).toBe(
        false,
      );
    });

    test("periode helt utenfor før valgtPeriode overlapper ikke", () => {
      expect(overlapperPeriode(valgtPeriode, { start: "2026-03-01", slutt: "2026-04-01" })).toBe(
        false,
      );
    });
  });

  describe("filtrerOverlappendePerioder", () => {
    test("returnerer alle elementer som overlapper valgt periode", () => {
      const liste = [
        { id: "innenfor", periode: { start: "2026-05-04", slutt: "2026-05-05" } },
        { id: "grense slutt", periode: { start: "2026-05-04", slutt: "2026-06-01" } },
        { id: "omslutter", periode: { start: "2026-04-01", slutt: "2026-07-01" } },
        { id: "utenfor 1", periode: { start: "2026-06-01", slutt: "2026-06-30" } },
        { id: "utenfor 2", periode: { start: "2026-04-01", slutt: "2026-05-01" } },
        { id: "utenfor 3", periode: { start: "2026-07-02", slutt: "2026-08-01" } },
      ];

      const outcome = filtrerOverlappendePerioder(valgtPeriode, liste);

      expect(outcome).toHaveLength(3);
      expect(outcome.map((it) => it.id)).toEqual(["innenfor", "grense slutt", "omslutter"]);
    });
  });
});
