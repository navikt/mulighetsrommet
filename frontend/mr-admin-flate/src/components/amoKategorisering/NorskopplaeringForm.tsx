import { FieldValues, Path } from "react-hook-form";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { FormCheckbox } from "@/components/skjema/FormCheckbox";

export function NorksopplaeringForm<T extends FieldValues>(props: {
  norskprovePath: Path<T>;
  innholdElementerPath: Path<T>;
  tiltakskode: Tiltakskode;
}) {
  const { norskprovePath, innholdElementerPath, tiltakskode } = props;

  return (
    <>
      <FormCheckbox<T> name={norskprovePath}>Gir mulighet for norskprøve</FormCheckbox>
      <InnholdElementerForm<T> path={innholdElementerPath} tiltakskode={tiltakskode} />
    </>
  );
}
