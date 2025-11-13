import { Select } from "@navikt/ds-react";
import { Suspense, useMemo } from "react";
import { useFormContext } from "react-hook-form";
import { useUtdanningsprogrammer } from "@/api/utdanning/useUtdanningsprogrammer";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { Laster } from "../laster/Laster";
import { AvtaleFormValues } from "@/schemas/avtale";

export function AvtaleUtdanningslopForm() {
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
  } = useFormContext<AvtaleFormValues>();

  const utdanningsprogram = watch("detaljer.utdanningslop.utdanningsprogram");
  const utdanningsprogrammer = useMemo(
    () => utdanninger.map((utdanning) => utdanning.utdanningsprogram),
    [utdanninger],
  );
  const utdanningerForUtdanningsprogram = utdanninger
    .filter((utdanning) => utdanning.utdanningsprogram.id === utdanningsprogram)
    .map((utdanning) => utdanning.utdanninger)
    .flat();

  return (
    <>
      <Select
        size="small"
        label={avtaletekster.utdanning.utdanningsprogram.label}
        {...register("detaljer.utdanningslop.utdanningsprogram")}
        onChange={(e) => {
          if (e.currentTarget.value !== utdanningsprogram) {
            setValue("detaljer.utdanningslop.utdanninger", []);
          }

          if (e.currentTarget.value !== "") {
            setValue("detaljer.utdanningslop.utdanningsprogram", e.currentTarget.value);
          } else {
            setValue("detaljer.utdanningslop", null);
          }
        }}
        error={errors.detaljer?.utdanningslop?.utdanninger?.message}
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
          {...register("detaljer.utdanningslop.utdanninger")}
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
