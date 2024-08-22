import { Checkbox, CheckboxGroup, HGrid, Select, UNSAFE_Combobox } from "@navikt/ds-react";
import {
  Avtale,
  ForerkortKlasse,
  InnholdElement,
  Sertifisering,
  Spesifisering,
  Toggles,
} from "@mr/api-client";
import { useFormContext } from "react-hook-form";
import { useFeatureToggle } from "../../api/features/useFeatureToggle";
import {
  forerkortKlasseToString,
  innholdElementToString,
  kurstypeToString,
  spesifiseringToString,
} from "@/utils/Utils";
import { InferredTiltaksgjennomforingSchema } from "@/components/redaksjoneltInnhold/TiltaksgjennomforingSchema";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";

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
      <Select readOnly size="small" label={tiltaktekster.kurstypeLabel} value={kurstype}>
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
          label={tiltaktekster.forerkortLabel}
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
          label={tiltaktekster.sertifiseringerLabel}
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
          {tiltaktekster.norskproveLabel}
        </Checkbox>
      )}
      {spesifisering && (
        <CheckboxGroup
          size="small"
          legend={tiltaktekster.innholdElementerLabel}
          error={errors?.amoKategorisering?.innholdElementer?.message}
          onChange={(values) => {
            setValue("amoKategorisering.innholdElementer", values);
          }}
          value={innholdElementer ?? []}
        >
          <HGrid columns={2}>
            <Checkbox value={InnholdElement.GRUNNLEGGENDE_FERDIGHETER}>
              {innholdElementToString(InnholdElement.GRUNNLEGGENDE_FERDIGHETER)}
            </Checkbox>
            <Checkbox value={InnholdElement.JOBBSOKER_KOMPETANSE}>
              {innholdElementToString(InnholdElement.JOBBSOKER_KOMPETANSE)}
            </Checkbox>
            <Checkbox value={InnholdElement.TEORETISK_OPPLAERING}>
              {innholdElementToString(InnholdElement.TEORETISK_OPPLAERING)}
            </Checkbox>
            <Checkbox value={InnholdElement.PRAKSIS}>
              {innholdElementToString(InnholdElement.PRAKSIS)}
            </Checkbox>
            <Checkbox value={InnholdElement.ARBEIDSMARKEDSKUNNSKAP}>
              {innholdElementToString(InnholdElement.ARBEIDSMARKEDSKUNNSKAP)}
            </Checkbox>
            <Checkbox value={InnholdElement.NORSKOPPLAERING}>
              {innholdElementToString(InnholdElement.NORSKOPPLAERING)}
            </Checkbox>
          </HGrid>
        </CheckboxGroup>
      )}
    </HGrid>
  );
}
