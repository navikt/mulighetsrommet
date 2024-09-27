import { Toggles } from "@mr/api-client";
import { Alert, Select } from "@navikt/ds-react";
import { useMemo } from "react";
import { useFormContext } from "react-hook-form";
import { useFeatureToggle } from "../../api/features/useFeatureToggle";
import { useHentUtdanninger } from "../../api/utdanning/useHentUtdanninger";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
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
    setValue,
    formState: { errors },
  } = useFormContext<InferredAvtaleSchema>();

  const watchedProgramomrade = watch("programomradeOgUtdanninger.programomradeId");
  const programomrader = useMemo(
    () => utdanninger?.map((utdanning) => utdanning.programomrade) ?? [],
    [utdanninger],
  );
  const utdanningerForProgramomrade =
    utdanninger
      ?.filter((utdanning) => utdanning.programomrade.id === watchedProgramomrade)
      .map((utdanning) => utdanning.utdanninger)
      .flat() || [];

  if (!enableUtdanningskjema) {
    return null;
  }

  if (isPending) {
    return (
      <Select disabled label="Velg programområde">
        <option>{avtaletekster.programomradeOgUtdanninger.laster}</option>
      </Select>
    );
  }

  if (!utdanninger) {
    return <Alert variant="warning">Klarte ikke hente programområder og utdanninger</Alert>;
  }

  return (
    <>
      <Select
        size="small"
        label={avtaletekster.programomradeOgUtdanninger.velgProgramomrade}
        {...register("programomradeOgUtdanninger.programomradeId")}
        onChange={(e) => {
          if (e.currentTarget.value !== watchedProgramomrade) {
            setValue("programomradeOgUtdanninger.utdanningsIder", []);
          }

          if (e.currentTarget.value !== "") {
            setValue("programomradeOgUtdanninger.programomradeId", e.currentTarget.value);
          } else if (e.currentTarget.value === "") {
            setValue("programomradeOgUtdanninger", null);
          }
        }}
        error={errors.programomradeOgUtdanninger?.programomradeId?.message}
      >
        <option value={""}>{avtaletekster.programomradeOgUtdanninger.velgProgramomrade}</option>
        {programomrader.map((programomrade) => (
          <option value={programomrade.id} key={programomrade.id}>
            {programomrade.navn}
          </option>
        ))}
      </Select>
      <pre>{JSON.stringify(watch("programomradeOgUtdanninger"), null, 2)}</pre>
      {watchedProgramomrade && (
        <ControlledMultiSelect
          size="small"
          placeholder={avtaletekster.programomradeOgUtdanninger.velgSluttkompetanser}
          label={avtaletekster.programomradeOgUtdanninger.velgSluttkompetanser}
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
