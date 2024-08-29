import { UNSAFE_Combobox } from "@navikt/ds-react";
import { ForerkortKlasse } from "@mr/api-client";
import { FieldValues, Path, PathValue, useFormContext } from "react-hook-form";
import { forerkortKlasseToString } from "../../utils/Utils";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";

export function ForerkortSkjema<T extends FieldValues>(props: {
  path: Path<T>;
  options?: ForerkortKlasse[];
}) {
  const { path, options } = props;
  const { setValue, watch } = useFormContext<T>();

  const forerkort = watch(path);

  return (
    <UNSAFE_Combobox
      clearButton
      size="small"
      label={tiltaktekster.forerkortLabel}
      isMultiSelect
      options={(
        options ?? [
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
        ]
      ).map((f) => ({
        label: forerkortKlasseToString(f),
        value: f,
      }))}
      selectedOptions={
        forerkort?.map((f: ForerkortKlasse) => ({
          label: forerkortKlasseToString(f),
          value: f,
        })) ?? []
      }
      onToggleSelected={(option, isSelected) =>
        isSelected
          ? setValue(path, [...(forerkort ?? []), option] as PathValue<T, Path<T>>)
          : setValue(
              path,
              forerkort?.filter((f: ForerkortKlasse) => f !== option),
            )
      }
    ></UNSAFE_Combobox>
  );
}
