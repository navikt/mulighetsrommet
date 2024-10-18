import { AvtaleDto } from "@mr/api-client";
import { Select } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { InferredTiltaksgjennomforingSchema } from "../redaksjoneltInnhold/TiltaksgjennomforingSchema";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { ControlledMultiSelect } from "@/components/skjema/ControlledMultiSelect";

interface Props {
  avtale: AvtaleDto;
}

export function TiltaksgjennomforingUtdanningslopSkjema({ avtale }: Props) {
  const { register } = useFormContext<InferredTiltaksgjennomforingSchema>();

  if (!avtale.utdanningslop) {
    return null;
  }

  return (
    <>
      <Select size="small" readOnly label={avtaletekster.utdanning.utdanningsprogram.label}>
        <option>{avtale.utdanningslop.utdanningsprogram.navn}</option>
      </Select>
      <ControlledMultiSelect
        size="small"
        label={avtaletekster.utdanning.laerefag.label}
        placeholder={avtaletekster.utdanning.laerefag.velg}
        {...register("utdanningslop.utdanninger")}
        options={avtale.utdanningslop.utdanninger.map((utdanning) => {
          return { value: utdanning.id, label: utdanning.navn };
        })}
      />
    </>
  );
}
