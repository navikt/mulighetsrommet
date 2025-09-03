import { TilsagnDto, TilsagnStatus, UtbetalingLinje } from "@tiltaksadministrasjon/api-client";
import { v4 as uuidv4 } from "uuid";

export function genrererUtbetalingLinjer(tilsagn: TilsagnDto[]): UtbetalingLinje[] {
  return tilsagn
    .filter((t) => t.status.type === TilsagnStatus.GODKJENT)
    .map((t) => toEmptyUtbetalingLinje(t))
    .toSorted(compareUtbetalingLinje);
}

function toEmptyUtbetalingLinje(tilsagn: TilsagnDto): UtbetalingLinje {
  return {
    id: uuidv4(),
    belop: 0,
    tilsagn: tilsagn,
    gjorOppTilsagn: false,
    status: null,
    opprettelse: null,
    handlinger: [],
  };
}

export function compareUtbetalingLinje(linje1: UtbetalingLinje, linje2: UtbetalingLinje): number {
  return linje1.tilsagn.bestillingsnummer.localeCompare(linje2.tilsagn.bestillingsnummer);
}
