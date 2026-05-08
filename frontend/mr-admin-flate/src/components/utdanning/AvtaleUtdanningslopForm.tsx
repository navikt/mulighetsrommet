import { Suspense, useMemo } from "react";
import { useFormContext } from "react-hook-form";
import { useUtdanningsprogrammer } from "@/api/utdanning/useUtdanningsprogrammer";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { FormComboboxMulti } from "@/components/skjema/FormComboboxMulti";
import { FormSelect } from "@/components/skjema/FormSelect";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { Laster } from "../laster/Laster";
import { AvtaleFormValues } from "@/pages/avtaler/form/validation";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { kreverUtdanningslop } from "@/utils/tiltakstype";

interface Props {
  tiltakskode: Tiltakskode;
}

export function AvtaleUtdanningslopForm({ tiltakskode }: Props) {
  if (!kreverUtdanningslop(tiltakskode)) {
    return null;
  }

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
  const { watch, setValue } = useFormContext<AvtaleFormValues>();

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
      <FormSelect
        label={avtaletekster.utdanning.utdanningsprogram.label}
        name={"detaljer.utdanningslop.utdanningsprogram"}
        rules={{
          onChange(e) {
            if (e.currentTarget.value !== utdanningsprogram) {
              setValue("detaljer.utdanningslop.utdanninger", []);
            }

            if (e.currentTarget.value !== "") {
              setValue("detaljer.utdanningslop.utdanningsprogram", e.currentTarget.value);
            } else {
              setValue("detaljer.utdanningslop", null);
            }
          },
        }}
      >
        <option value={""}>{avtaletekster.utdanning.utdanningsprogram.velg}</option>
        {utdanningsprogrammer.map((utdanningsprogram) => (
          <option value={utdanningsprogram.id} key={utdanningsprogram.id}>
            {utdanningsprogram.navn}
          </option>
        ))}
      </FormSelect>
      {utdanningsprogram && (
        <FormComboboxMulti<AvtaleFormValues>
          label={avtaletekster.utdanning.laerefag.label}
          placeholder={avtaletekster.utdanning.laerefag.velg}
          name="detaljer.utdanningslop.utdanninger"
          options={utdanningerForUtdanningsprogram.map((utdanning) => ({
            value: utdanning.id,
            label: utdanning.navn,
          }))}
        />
      )}
    </>
  );
}
