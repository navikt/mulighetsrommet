import { AvtaleDto } from "@mr/api-client";
import { Select } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { InferredTiltaksgjennomforingSchema } from "../redaksjoneltInnhold/TiltaksgjennomforingSchema";

interface Props {
  avtale: AvtaleDto;
}

export function TiltaksgjennomforingUtdanningskategorierSkjema({ avtale }: Props) {
  const {
    watch,
    setValue,
    formState: { errors },
  } = useFormContext<InferredTiltaksgjennomforingSchema>();
  return (
    <>
      <Select size="small" readOnly label="Programområde">
        <option>{avtale.programomradeMedUtdanninger?.programomrade.navn}</option>
      </Select>
      <Select
        onChange={(e) => {
          setValue("programomradeOgUtdanninger.utdanningsIder", [e.currentTarget.value]);
        }}
        value={watch("programomradeOgUtdanninger.utdanningsIder")}
        error={errors.programomradeOgUtdanninger?.utdanningsIder?.message}
        size="small"
        label="Velg sluttkompetanse"
      >
        <option value={undefined}>Velg sluttkompetanse</option>
        {avtale.programomradeMedUtdanninger?.utdanninger.map((ut) => {
          return (
            <option key={ut.id} value={ut.id}>
              {ut.navn}
            </option>
          );
        })}
      </Select>
    </>
  );
}
