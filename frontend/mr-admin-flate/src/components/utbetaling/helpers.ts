import { DelutbetalingRequest, UtbetalingLinje } from "@tiltaksadministrasjon/api-client";

export type RedigerUtbetalingLinjeFormValues = {
  formLinjer: UtbetalingLinje[];
};

export type UpdatedLinje = { index: number; linje: UtbetalingLinje };

export function getUpdatedLinjer(
  formList: UtbetalingLinje[],
  apiLinjer: UtbetalingLinje[],
): UpdatedLinje[] {
  return formList.flatMap((linje, index) => {
    const apiLinje = apiLinjer.find(({ id }) => id === linje.id);

    if (!apiLinje || apiLinje.status === linje.status) {
      return [];
    } else {
      return [
        { index, linje: { ...apiLinje, belop: linje.belop, gjorOppTilsagn: linje.gjorOppTilsagn } },
      ];
    }
  });
}
export function getChangeSet(
  formList: UtbetalingLinje[],
  apiLinjer: UtbetalingLinje[],
): { updatedLinjer: UpdatedLinje[]; newLinjer: UtbetalingLinje[] } {
  const updatedLinjer = getUpdatedLinjer(formList, apiLinjer);
  const newLinjer = apiLinjer.filter((apiLinje) => !formList.some(({ id }) => id === apiLinje.id));
  return { updatedLinjer, newLinjer };
}

export function toDelutbetaling(linje: UtbetalingLinje): DelutbetalingRequest {
  return {
    id: linje.id,
    tilsagnId: linje.tilsagn.id,
    belop: linje.belop,
    gjorOppTilsagn: linje.gjorOppTilsagn,
  };
}
