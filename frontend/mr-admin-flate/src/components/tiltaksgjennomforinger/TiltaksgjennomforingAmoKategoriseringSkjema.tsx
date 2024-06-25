import { Checkbox, CheckboxGroup, HGrid, Select, UNSAFE_Combobox } from "@navikt/ds-react";
import {
  Avtale,
  ForerkortKlasse,
  InnholdElement,
  Sertifisering,
  Spesifisering,
  Toggles,
} from "mulighetsrommet-api-client";
import { useFormContext } from "react-hook-form";
import { useFeatureToggle } from "../../api/features/useFeatureToggle";
import {
  forerkortKlasseToString,
  kurstypeToString,
  spesifiseringToString,
} from "../../utils/Utils";
import { InferredTiltaksgjennomforingSchema } from "../redaksjonelt-innhold/TiltaksgjennomforingSchema";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";

interface Props {
  avtale: Avtale;
}

export function TiltaksgjennomforingAmoKategoriseringSkjema(props: Props) {
  const { avtale } = props;
  const { data: isEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_GRUPPE_AMO_KATEGORIER,
  );

  const {
    setValue,
    register,
    watch,
    formState: { errors },
  } = useFormContext<InferredTiltaksgjennomforingSchema>();

  if (!isEnabled || !avtale.amoKategorisering) {
    return null;
  }

  const kurstype = avtale.amoKategorisering.kurstype;
  const spesifisering = avtale.amoKategorisering.spesifisering;
  const avtaleSertifiseringer = avtale.amoKategorisering.sertifiseringer;
  const avtaleForerkort = avtale.amoKategorisering.forerkort;
  const forerkort = watch("amoKategorisering.forerkort");
  const norskprove = watch("amoKategorisering.norskprove");
  const innholdElementer = watch("amoKategorisering.innholdElementer");

  return (
    <HGrid gap="4" columns={1}>
      <Select readOnly size="small" label="Kurstype" value={kurstype}>
        <option>{kurstypeToString(kurstype)}</option>
      </Select>
      {spesifisering && (
        <Select readOnly size="small" label="Spesifisering" hideLabel value={spesifisering}>
          <option>{spesifiseringToString(spesifisering)}</option>
        </Select>
      )}
      {avtaleForerkort && avtaleForerkort.length > 0 && (
        <UNSAFE_Combobox
          clearButton
          size="small"
          label="Førerkort"
          isMultiSelect
          options={avtaleForerkort.map((f) => ({
            label: forerkortKlasseToString(f),
            value: f,
          }))}
          selectedOptions={
            forerkort?.map((f) => ({
              label: forerkortKlasseToString(f),
              value: f,
            })) ?? []
          }
          onToggleSelected={(option, isSelected) =>
            isSelected
              ? setValue("amoKategorisering.forerkort", [
                  ...(forerkort ?? []),
                  option as ForerkortKlasse,
                ])
              : setValue(
                  "amoKategorisering.forerkort",
                  forerkort?.filter((f) => f !== option),
                )
          }
        ></UNSAFE_Combobox>
      )}
      {avtaleSertifiseringer && avtaleSertifiseringer.length > 0 && (
        <ControlledMultiSelect<{ konseptId: number; label: string }>
          size="small"
          placeholder="Søk etter sertifiseringer"
          label={"Sertifiseringer"}
          {...register("amoKategorisering.sertifiseringer")}
          options={
            avtaleSertifiseringer.map((s: Sertifisering) => ({
              value: s,
              label: s.label,
            })) ?? []
          }
        />
      )}
      {spesifisering === Spesifisering.NORSKOPPLAERING && (
        <Checkbox
          value={norskprove ?? false}
          onChange={() => setValue("amoKategorisering.norskprove", !(norskprove ?? false))}
          size="small"
        >
          Norskprøve
        </Checkbox>
      )}
      {spesifisering && (
        <CheckboxGroup
          size="small"
          legend="Elementer i kurset"
          error={errors?.amoKategorisering?.innholdElementer?.message}
          onChange={(values) => {
            setValue("amoKategorisering.innholdElementer", values);
          }}
          value={innholdElementer ?? []}
        >
          <HGrid columns={2}>
            <Checkbox value={InnholdElement.GRUNNLEGGENDE_FERDIGHETER}>
              Grunnleggende ferdigheter
            </Checkbox>
            <Checkbox value={InnholdElement.JOBBSOKER_KOMPETANSE}>Jobbsøkerkompetanse</Checkbox>
            <Checkbox value={InnholdElement.TEORETISK_OPPLAERING}>Teoretisk opplæring</Checkbox>
            <Checkbox value={InnholdElement.PRAKSIS}>Praksis</Checkbox>
            <Checkbox value={InnholdElement.ARBEIDSMARKEDSKUNNSKAP}>
              Arbeidsmarkedskunnskap
            </Checkbox>
            <Checkbox value={InnholdElement.NORSKOPPLAERING}>Norskopplæring</Checkbox>
          </HGrid>
        </CheckboxGroup>
      )}
    </HGrid>
  );
}
