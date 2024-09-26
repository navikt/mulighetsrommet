import { Toggles } from "@mr/api-client";
import { Alert, Select } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { useFeatureToggle } from "../../api/features/useFeatureToggle";
import { useHentUtdanninger } from "../../api/utdanning/useHentUtdanninger";
import { InferredAvtaleSchema } from "../redaksjoneltInnhold/AvtaleSchema";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";

export function AvtaleUtdanningSkjema() {
  const { data: enableUtdanningskjema } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_UTDANNINGSKATEGORIER,
  );
  const { data: utdanninger, isPending } = useHentUtdanninger();
  const {
    register,
    watch,
    formState: { errors },
  } = useFormContext<InferredAvtaleSchema>();

  if (!enableUtdanningskjema) {
    return null;
  }

  if (isPending) {
    return (
      <Select disabled label="Velg programområde">
        <option>Laster...</option>
      </Select>
    );
  }

  if (!utdanninger) {
    return <Alert variant="warning">Klarte ikke hente programområder og utdanninger</Alert>;
  }

  const watchedProgramomrade = watch("programomradeOgUtdanninger.programomradeId");
  const programomrader = utdanninger.map((utdanning) => utdanning.programomrade);
  const utdanningerForProgramomrade = utdanninger
    .filter((utdanning) => utdanning.programomrade.id === watchedProgramomrade)
    .map((utdanning) => utdanning.utdanninger)
    .flat();

  return (
    <>
      <pre>{JSON.stringify(watch("programomradeOgUtdanninger"), null, 2)}</pre>
      <Select
        size="small"
        label="Velg programområde"
        {...register("programomradeOgUtdanninger.programomradeId")}
        error={errors.programomradeOgUtdanninger?.programomradeId?.message}
      >
        {programomrader.map((programomrade) => (
          <option value={programomrade.id} key={programomrade.id}>
            {programomrade.navn}
          </option>
        ))}
      </Select>
      {watchedProgramomrade && (
        <ControlledMultiSelect
          size="small"
          placeholder="Velg sluttkompetanser"
          label="Velg sluttkompetanser"
          {...register("programomradeOgUtdanninger.utdanningsIder")}
          options={utdanningerForProgramomrade.map((utdanning) => {
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
