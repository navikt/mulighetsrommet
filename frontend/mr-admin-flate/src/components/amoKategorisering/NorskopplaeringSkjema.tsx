import { Checkbox } from "@navikt/ds-react";
import { FieldValues, Path, PathValue, useFormContext } from "react-hook-form";
import { InnholdElementerSkjema } from "./InnholdElementerSkjema";

export function NorksopplaeringSkjema<T extends FieldValues>(props: {
  norskprovePath: Path<T>;
  innholdElementerPath: Path<T>;
}) {
  const { norskprovePath, innholdElementerPath } = props;
  const { watch, setValue } = useFormContext<T>();

  const norskprove = watch(norskprovePath);
  return (
    <>
      <Checkbox
        checked={norskprove ?? false}
        onChange={() => setValue(norskprovePath, !(norskprove ?? false) as PathValue<T, Path<T>>)}
        size="small"
      >
        Gir mulighet for norskprøve
      </Checkbox>
      <InnholdElementerSkjema<T> path={innholdElementerPath} />
    </>
  );
}
