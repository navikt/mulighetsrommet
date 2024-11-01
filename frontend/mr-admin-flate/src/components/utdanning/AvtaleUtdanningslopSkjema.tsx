import { Select } from "@navikt/ds-react";
import { Suspense, useMemo } from "react";
import { useFormContext } from "react-hook-form";
import { useUtdanningsprogrammer } from "@/api/utdanning/useUtdanningsprogrammer";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "../redaksjoneltInnhold/AvtaleSchema";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { ReloadAppErrorBoundary } from "@mr/frontend-common";
import { Laster } from "../laster/Laster";

export function AvtaleUtdanningslopSkjema() {
  return (
    <ReloadAppErrorBoundary>
      <Suspense fallback={<Laster />}>
        <SelectAvtaleUtdanning />
      </Suspense>
    </ReloadAppErrorBoundary>
  );
}

function SelectAvtaleUtdanning() {
  const { data: utdanninger } = useUtdanningsprogrammer();
  const {
    register,
    watch,
    setValue,
    formState: { errors },
  } = useFormContext<InferredAvtaleSchema>();

  const utdanningsprogram = watch("utdanningslop.utdanningsprogram");
  const utdanningsprogrammer = useMemo(
    () => utdanninger.map((utdanning) => utdanning.utdanningsprogram),
    [utdanninger],
  );
  const utdanningerForUtdanningsprogram =
    utdanninger
      .filter((utdanning) => utdanning.utdanningsprogram.id === utdanningsprogram)
      .map((utdanning) => utdanning.utdanninger)
      .flat() || [];

  return (
    <>
      <Select
        size="small"
        label={avtaletekster.utdanning.utdanningsprogram.label}
        {...register("utdanningslop.utdanningsprogram")}
        onChange={(e) => {
          if (e.currentTarget.value !== utdanningsprogram) {
            setValue("utdanningslop.utdanninger", []);
          }

          if (e.currentTarget.value !== "") {
            setValue("utdanningslop.utdanningsprogram", e.currentTarget.value);
          } else if (e.currentTarget.value === "") {
            setValue("utdanningslop", null);
          }
        }}
        error={errors.utdanningslop?.utdanningsprogram?.message}
      >
        <option value={""}>{avtaletekster.utdanning.utdanningsprogram.velg}</option>
        {utdanningsprogrammer.map((utdanningsprogram) => (
          <option value={utdanningsprogram.id} key={utdanningsprogram.id}>
            {utdanningsprogram.navn}
          </option>
        ))}
      </Select>
      {utdanningsprogram && (
        <ControlledMultiSelect
          size="small"
          label={avtaletekster.utdanning.laerefag.label}
          placeholder={avtaletekster.utdanning.laerefag.velg}
          {...register("utdanningslop.utdanninger")}
          options={utdanningerForUtdanningsprogram.map((utdanning) => {
            return {
              value: utdanning.id,
              label: utdanning.navn,
            };
          })}
        />
      )}
    </>
  );
}
