import { Periode } from "@api-client";
import { describe, expect, test } from "vitest";
import { innenforValgtPeriode, overlapperSluttAvPeriode, overlapperStartAvPeriode } from "./periode-filtrering";

describe("periode filtrering", () => {
  describe("overlapperStartAvPeriode", () => {
    test("inkluderende startdato", () => {
      const periodeA: Periode = { start: "2025-09-01", slutt: "2025-10-01" };
      const periodeB: Periode = { start: "2025-09-01", slutt: "2025-09-30" };

      const outcome = overlapperStartAvPeriode(periodeA, periodeB);
      expect(outcome).toBeTruthy();
    });
    test("ekskluderende sluttdato", () => {
      const periodeA: Periode = { start: "2025-09-01", slutt: "2025-10-01" };
      const periodeB: Periode = { start: "2025-08-01", slutt: "2025-09-01" };

      const outcome = overlapperStartAvPeriode(periodeA, periodeB);
      expect(outcome).toBeFalsy();
    });
  });

  describe("overlapperSluttAvPeriode", () => {
    test("sluttdato i intervallet", () => {
      const periodeA: Periode = { start: "2025-08-01", slutt: "2025-09-15" };
      const periodeB: Periode = { start: "2025-09-01", slutt: "2025-10-01" };

      const outcome = overlapperSluttAvPeriode(periodeA, periodeB);
      expect(outcome).toBeTruthy();
    });

    test("sluttdato på et av datoene", () => {
      const periodeA: Periode = { start: "2025-08-01", slutt: "2025-09-01" };
      const periodeB: Periode = { start: "2025-09-01", slutt: "2025-10-01" };

      const outcome = overlapperSluttAvPeriode(periodeA, periodeB);
      expect(outcome).toBeFalsy();
    });
  });

  describe("periode innenfor valgt periode", () => {
    test("periode innenfor valgt periode", () => {
      const periodeA: Periode = { start: "2025-08-01", slutt: "2025-10-01" };
      const periodeB: Periode = { start: "2025-08-02", slutt: "2025-09-30" };

      const outcome = innenforValgtPeriode(periodeA, periodeB);
      expect(outcome).toBeTruthy();
    });

    test("start er ikke innenfor valgt periode", () => {
      const periodeA: Periode = { start: "2025-07-01", slutt: "2025-10-01" };
      const periodeB: Periode = { start: "2025-07-01", slutt: "2025-09-30" };

      const outcome = innenforValgtPeriode(periodeA, periodeB);
      expect(outcome).toBeFalsy();
    });

    test("slutt er ikke innenfor valgt periode", () => {
      const periodeA: Periode = { start: "2025-07-01", slutt: "2025-10-01" };
      const periodeB: Periode = { start: "2025-07-02", slutt: "2025-10-15" };

      const outcome = innenforValgtPeriode(periodeA, periodeB);
      expect(outcome).toBeFalsy();
    });
  });
});
