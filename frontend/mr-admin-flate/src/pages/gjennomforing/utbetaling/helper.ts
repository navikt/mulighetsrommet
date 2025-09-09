import { UtbetalingLinje } from "@tiltaksadministrasjon/api-client";

export type UtbetalingLinjerState = {
  linjer: UtbetalingLinje[];
};

export type UtbetalingLinjerStateAction =
  | { type: "REFETCH" }
  | { type: "REMOVE"; id: string }
  | { type: "UPDATE"; linje: UtbetalingLinje };
