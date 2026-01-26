import { DelutbetalingRequest, UtbetalingLinje } from "@tiltaksadministrasjon/api-client";

export type RedigerUtbetalingLinjeFormValues = {
  formLinjer: UtbetalingLinje[];
};

export function toDelutbetaling(linje: UtbetalingLinje): DelutbetalingRequest {
  return {
    id: linje.id,
    tilsagnId: linje.tilsagn.id,
    pris: linje.pris,
    gjorOppTilsagn: linje.gjorOppTilsagn,
  };
}
