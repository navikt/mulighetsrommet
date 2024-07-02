import { Checkbox, CheckboxGroup, HGrid, Select, UNSAFE_Combobox } from "@navikt/ds-react";
import {
  ForerkortKlasse,
  InnholdElement,
  Kurstype,
  Sertifisering,
  Spesifisering,
} from "mulighetsrommet-api-client";
import { useFormContext } from "react-hook-form";
import {
  forerkortKlasseToString,
  innholdElementToString,
  kurstypeToString,
  spesifiseringToString,
} from "../../utils/Utils";
import { InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";
import { useState } from "react";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { useSokSertifiseringer } from "@/api/janzz/useSokSertifiseringer";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";

export function AvtaleAmoKategoriseringSkjema() {
  const [janzzQuery, setJanzzQuery] = useState<string>("");
  const { data: sertifiseringerFraSok } = useSokSertifiseringer(janzzQuery);

  const {
    setValue,
    watch,
    register,
    formState: { errors },
  } = useFormContext<InferredAvtaleSchema>();

  const kurstype = watch("amoKategorisering.kurstype");
  const spesifisering = watch("amoKategorisering.spesifisering");
  const isBransjeSpesifisering =
    spesifisering &&
    [
      Spesifisering.SERVERING_OVERNATTING,
      Spesifisering.TRANSPORT,
      Spesifisering.INDUSTRI,
      Spesifisering.ANDRE_BRANSJER,
    ].includes(spesifisering);
  const forerkort = watch("amoKategorisering.forerkort");
  const norskprove = watch("amoKategorisering.norskprove");
  const innholdElementer = watch("amoKategorisering.innholdElementer");
  const sertifiseringer = watch("amoKategorisering.sertifiseringer");

  function sertifiseringerOptions() {
    const options =
      sertifiseringer?.map((s) => ({
        label: s.label,
        value: s,
      })) ?? [];

    sertifiseringerFraSok
      ?.filter((s: Sertifisering) => !options.some((o) => o.value.konseptId === s.konseptId))
      ?.forEach((s: Sertifisering) =>
        options.push({
          label: s.label,
          value: { konseptId: s.konseptId, label: s.label },
        }),
      );

    return options;
  }

  return (
    <HGrid gap="4" columns={1}>
      <Select
        size="small"
        label={tiltaktekster.kurstypeLabel}
        value={kurstype}
        error={errors?.amoKategorisering?.kurstype?.message}
        onChange={(type) => {
          setValue("amoKategorisering.kurstype", type.target.value as Kurstype);
          setValue("amoKategorisering.norskprove", undefined);
          setValue("amoKategorisering.spesifisering", undefined);
          setValue("amoKategorisering.forerkort", undefined);
          setValue("amoKategorisering.sertifiseringer", undefined);
          setValue("amoKategorisering.innholdElementer", undefined);
        }}
      >
        <option value={undefined}>Velg kurstype</option>
        <option value={Kurstype.BRANSJE}>{kurstypeToString(Kurstype.BRANSJE)}</option>
        <option value={Kurstype.NORSKOPPLAERING}>
          {kurstypeToString(Kurstype.NORSKOPPLAERING)}
        </option>
        <option value={Kurstype.STUDIESPESIALISERING}>
          {kurstypeToString(Kurstype.STUDIESPESIALISERING)}
        </option>
      </Select>
      {kurstype === Kurstype.BRANSJE && (
        <Select
          size="small"
          label="Spesifisering"
          hideLabel
          value={spesifisering}
          error={errors?.amoKategorisering?.spesifisering?.message}
          onChange={(type) => {
            setValue("amoKategorisering.spesifisering", type.target.value as Spesifisering);
          }}
        >
          <option value={undefined}>Velg spesifisering</option>
          <option value={Spesifisering.SERVERING_OVERNATTING}>
            {spesifiseringToString(Spesifisering.SERVERING_OVERNATTING)}
          </option>
          <option value={Spesifisering.TRANSPORT}>
            {spesifiseringToString(Spesifisering.TRANSPORT)}
          </option>
          <option value={Spesifisering.INDUSTRI}>
            {spesifiseringToString(Spesifisering.INDUSTRI)}
          </option>
          <option value={Spesifisering.ANDRE_BRANSJER}>
            {spesifiseringToString(Spesifisering.ANDRE_BRANSJER)}
          </option>
        </Select>
      )}
      {kurstype === Kurstype.NORSKOPPLAERING && (
        <Select
          size="small"
          label="Spesifisering"
          hideLabel
          value={spesifisering}
          error={errors?.amoKategorisering?.spesifisering?.message}
          onChange={(type) => {
            setValue("amoKategorisering.spesifisering", type.target.value as Spesifisering);
            setValue("amoKategorisering.norskprove", undefined);
            setValue("amoKategorisering.innholdElementer", undefined);
          }}
        >
          <option value={undefined}>Velg spesifisering</option>
          <option value={Spesifisering.NORSKOPPLAERING}>
            {spesifiseringToString(Spesifisering.NORSKOPPLAERING)}
          </option>
          <option value={Spesifisering.GRUNNLEGGENDE_FERDIGHETER}>
            {spesifiseringToString(Spesifisering.GRUNNLEGGENDE_FERDIGHETER)}
          </option>
        </Select>
      )}
      {isBransjeSpesifisering && (
        <UNSAFE_Combobox
          clearButton
          size="small"
          label={tiltaktekster.forerkortLabel}
          isMultiSelect
          options={[
            ForerkortKlasse.A,
            ForerkortKlasse.A1,
            ForerkortKlasse.A2,
            ForerkortKlasse.AM,
            ForerkortKlasse.AM_147,
            ForerkortKlasse.B,
            ForerkortKlasse.B_78,
            ForerkortKlasse.BE,
            ForerkortKlasse.C,
            ForerkortKlasse.C1,
            ForerkortKlasse.C1E,
            ForerkortKlasse.CE,
            ForerkortKlasse.D,
            ForerkortKlasse.D1,
            ForerkortKlasse.D1E,
            ForerkortKlasse.DE,
            ForerkortKlasse.S,
            ForerkortKlasse.T,
          ].map((f) => ({
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
      {isBransjeSpesifisering && (
        <ControlledMultiSelect<{ konseptId: number; label: string }>
          size="small"
          placeholder="Søk etter sertifiseringer"
          label={tiltaktekster.sertifiseringerLabel}
          {...register("amoKategorisering.sertifiseringer")}
          onInputChange={(s: string) => {
            setJanzzQuery(s);
          }}
          options={sertifiseringerOptions()}
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
