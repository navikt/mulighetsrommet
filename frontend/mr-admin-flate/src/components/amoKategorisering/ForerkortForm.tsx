import { AmoKategoriseringBransjeOgYrkesrettetForerkortKlasse as ForerkortKlasse } from "@tiltaksadministrasjon/api-client";
import { FieldValues, Path } from "react-hook-form";
import { forerkortKlasseToString } from "@/utils/Utils";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { FormCombobox } from "@/components/skjema/FormCombobox";

interface ForerkortFormProps<T extends FieldValues> {
  path: Path<T>;
  options?: ForerkortKlasse[];
}

export function ForerkortForm<T extends FieldValues>(props: ForerkortFormProps<T>) {
  const { path, options = defaultOptions } = props;

  const labeledOptions = options.map((forerkort) => ({
    label: forerkortKlasseToString(forerkort),
    value: forerkort,
  }));

  return (
    <FormCombobox<T>
      name={path}
      label={gjennomforingTekster.forerkortLabel}
      isMultiSelect
      options={labeledOptions}
    />
  );
}

const defaultOptions = [
  ForerkortKlasse.A,
  ForerkortKlasse.A1,
  ForerkortKlasse.A2,
  ForerkortKlasse.AM,
  ForerkortKlasse.AM_147,
  ForerkortKlasse.B,
  ForerkortKlasse.B_78,
  ForerkortKlasse.BE,
  ForerkortKlasse.C,
  ForerkortKlasse.C1,
  ForerkortKlasse.C1E,
  ForerkortKlasse.CE,
  ForerkortKlasse.D,
  ForerkortKlasse.D1,
  ForerkortKlasse.D1E,
  ForerkortKlasse.DE,
  ForerkortKlasse.S,
  ForerkortKlasse.T,
];
