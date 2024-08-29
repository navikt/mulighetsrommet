import { Sertifisering } from "@mr/api-client";
import { FieldValues, Path, useFormContext } from "react-hook-form";
import { useState } from "react";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { useSokSertifiseringer } from "@/api/janzz/useSokSertifiseringer";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";
import { SelectOption } from "@mr/frontend-common/components/SokeSelect";

export function SertifiseringerSkjema<T extends FieldValues>(props: {
  path: Path<T>;
  options?: Sertifisering[];
}) {
  const { path, options } = props;
  const [janzzQuery, setJanzzQuery] = useState<string>("");
  const { data: sertifiseringerFraSok } = useSokSertifiseringer(janzzQuery);
  const { watch, register } = useFormContext<T>();

  const sertifiseringer = watch(path);

  function sertifiseringerOptions() {
    const selected =
      sertifiseringer?.map((s: Sertifisering) => ({
        label: s.label,
        value: s,
      })) ?? [];

    if (!options) {
      sertifiseringerFraSok
        ?.filter(
          (s: Sertifisering) =>
            !selected.some((o: SelectOption<Sertifisering>) => o.value.konseptId === s.konseptId),
        )
        ?.forEach((s: Sertifisering) =>
          selected.push({
            label: s.label,
            value: { konseptId: s.konseptId, label: s.label },
          }),
        );
    } else {
      options
        ?.filter(
          (s: Sertifisering) =>
            !selected.some((o: SelectOption<Sertifisering>) => o.value.konseptId === s.konseptId),
        )
        ?.forEach((s: Sertifisering) =>
          selected.push({
            label: s.label,
            value: { konseptId: s.konseptId, label: s.label },
          }),
        );
    }

    return selected;
  }

  return (
    <ControlledMultiSelect<{ konseptId: number; label: string }>
      size="small"
      placeholder="Søk etter sertifiseringer"
      label={tiltaktekster.sertifiseringerLabel}
      {...register(path)}
      onInputChange={(s: string) => {
        if (!options) {
          setJanzzQuery(s);
        }
      }}
      options={sertifiseringerOptions()}
    />
  );
}
