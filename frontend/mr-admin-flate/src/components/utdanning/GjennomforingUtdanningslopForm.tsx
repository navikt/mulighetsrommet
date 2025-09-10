import { Alert, Select } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { InferredGjennomforingSchema } from "../redaksjoneltInnhold/GjennomforingSchema";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { ControlledMultiSelect } from "@/components/skjema/ControlledMultiSelect";
import { AvtaleDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  avtale: AvtaleDto;
}

export function GjennomforingUtdanningslopForm({ avtale }: Props) {
  const { register } = useFormContext<InferredGjennomforingSchema>();

  if (!avtale.utdanningslop) {
    return (
      <Alert variant="warning">{avtaletekster.utdanning.utdanningsprogramManglerForAvtale}</Alert>
    );
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
