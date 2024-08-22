import { Checkbox, CheckboxGroup, HGrid, List, Select, UNSAFE_Combobox } from "@navikt/ds-react";
import {
  ForerkortKlasse,
  InnholdElement,
  Kurstype,
  Sertifisering,
  Spesifisering,
} from "@mr/api-client";
import { useFormContext } from "react-hook-form";
import {
  forerkortKlasseToString,
  innholdElementToString,
  kurstypeToString,
  spesifiseringToString,
} from "../../utils/Utils";
import { InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";
import { useState } from "react";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { useSokSertifiseringer } from "@/api/janzz/useSokSertifiseringer";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";

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
  const isBransje = kurstype === Kurstype.BRANSJE;
  const forerkort = watch("amoKategorisering.forerkort");
  const norskprove = watch("amoKategorisering.norskprove");
  const innholdElementer = watch("amoKategorisering.innholdElementer");
  const sertifiseringer = watch("amoKategorisering.sertifiseringer");

  function sertifiseringerOptions() {
    const options =
      sertifiseringer?.map((s: Sertifisering) => ({
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
      {isBransje && (
        <Select
          size="small"
          label={
            <LabelWithHelpText label="Bransje" helpTextTitle="Bransje oversikt" placement="right">
              <BransjeHelpText />
            </LabelWithHelpText>
          }
          value={spesifisering}
          error={errors?.amoKategorisering?.spesifisering?.message}
          onChange={(type) => {
            setValue("amoKategorisering.spesifisering", type.target.value as Spesifisering);
          }}
        >
          <option value={undefined}>Velg bransje</option>
          <option value={Spesifisering.INGENIOR_OG_IKT_FAG}>
            {spesifiseringToString(Spesifisering.INGENIOR_OG_IKT_FAG)}
          </option>
          <option value={Spesifisering.HELSE_PLEIE_OG_OMSORG}>
            {spesifiseringToString(Spesifisering.HELSE_PLEIE_OG_OMSORG)}
          </option>
          <option value={Spesifisering.BARNE_OG_UNGDOMSARBEID}>
            {spesifiseringToString(Spesifisering.BARNE_OG_UNGDOMSARBEID)}
          </option>
          <option value={Spesifisering.KONTORARBEID}>
            {spesifiseringToString(Spesifisering.KONTORARBEID)}
          </option>
          <option value={Spesifisering.BUTIKK_OG_SALGSARBEID}>
            {spesifiseringToString(Spesifisering.BUTIKK_OG_SALGSARBEID)}
          </option>
          <option value={Spesifisering.BYGG_OG_ANLEGG}>
            {spesifiseringToString(Spesifisering.BYGG_OG_ANLEGG)}
          </option>
          <option value={Spesifisering.INDUSTRIARBEID}>
            {spesifiseringToString(Spesifisering.INDUSTRIARBEID)}
          </option>
          <option value={Spesifisering.REISELIV_SERVERING_OG_TRANSPORT}>
            {spesifiseringToString(Spesifisering.REISELIV_SERVERING_OG_TRANSPORT)}
          </option>
          <option value={Spesifisering.SERVICEYRKER_OG_ANNET_ARBEID}>
            {spesifiseringToString(Spesifisering.SERVICEYRKER_OG_ANNET_ARBEID)}
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
          <option value={Spesifisering.FORBEREDENDE_OPPLAERING_FOR_VOKSNE}>
            {spesifiseringToString(Spesifisering.FORBEREDENDE_OPPLAERING_FOR_VOKSNE)}
          </option>
          <option value={Spesifisering.GRUNNLEGGENDE_FERDIGHETER}>
            {spesifiseringToString(Spesifisering.GRUNNLEGGENDE_FERDIGHETER)}
          </option>
        </Select>
      )}
      {isBransje && (
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
      {isBransje && (
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
          checked={norskprove ?? false}
          onChange={() => setValue("amoKategorisering.norskprove", !(norskprove ?? false))}
          size="small"
        >
          Gir mulighet for norskprøve
        </Checkbox>
      )}
      {spesifisering && spesifisering !== Spesifisering.FORBEREDENDE_OPPLAERING_FOR_VOKSNE && (
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

function BransjeHelpText() {
  return (
    <div
      style={{
        maxHeight: "400px",
        overflow: "auto",
      }}
    >
      <List as="ul" size="small" title="Ingeniør- og ikt-fag">
        <List.Item>Andre naturvitenskapelige yrker</List.Item>
        <List.Item>Ikt-yrker</List.Item>
        <List.Item>Ingeniører og teknikere</List.Item>
      </List>
      <List as="ul" size="small" title="Helse, pleie og omsorg">
        <List.Item>Omsorgs- og pleiearbeidere</List.Item>
        <List.Item>Annet helsepersonell</List.Item>
        <List.Item>Mellomledere innen helse, pleie og omsorg</List.Item>
      </List>
      <List as="ul" size="small" title="Barne- og ungdomsarbeid">
        <List.Item>Skoleassistenter</List.Item>
        <List.Item>Barnehage- og skolefritidsassistenter</List.Item>
      </List>
      <List as="ul" size="small" title="Kontorarbeid">
        <List.Item>Lavere saksbehandlere innen offentlig administrasjon</List.Item>
        <List.Item>Sekretærer</List.Item>
        <List.Item>Økonomi- og kontormedarbeidere</List.Item>
        <List.Item>Lager- og transportmedarbeidere</List.Item>
        <List.Item>Resepsjonister og sentralbordoperatører</List.Item>
        <List.Item>Andre funksjonærer</List.Item>
      </List>
      <List as="ul" size="small" title="Butikk- og salgsarbeid">
        <List.Item>Butikkarbeid</List.Item>
        <List.Item>Annet salgsarbeid</List.Item>
      </List>
      <List as="ul" size="small" title="Bygg og anlegg">
        <List.Item>Rørleggere</List.Item>
        <List.Item>Snekkere og tømrere</List.Item>
        <List.Item>Elektrikere</List.Item>
        <List.Item>Andre bygningsarbeidere</List.Item>
        <List.Item>Anleggsarbeidere</List.Item>
        <List.Item>Hjelpearbeidere innen bygg og anlegg</List.Item>
        <List.Item>Mellomledere innen bygg og anlegg</List.Item>
      </List>
      <List as="ul" size="small" title="Industriarbeid">
        <List.Item>Mekanikere</List.Item>
        <List.Item>Prosess- og maskinoperatører</List.Item>
        <List.Item>Næringsmiddelarbeid</List.Item>
        <List.Item>Automatikere og elektriske montører</List.Item>
        <List.Item>Andre håndverkere</List.Item>
        <List.Item>Hjelpearbeid innen industrien</List.Item>
        <List.Item>Mellomledere innen industriarbeid</List.Item>
      </List>
      <List as="ul" size="small" title="Reiseliv, servering og transport">
        <List.Item>Maritime yrker</List.Item>
        <List.Item>Førere av transportmidler</List.Item>
        <List.Item>Reiseledere, guider og reisebyråmedarbeidere</List.Item>
        <List.Item>Konduktører og kabinpersonale</List.Item>
        <List.Item>Kokker</List.Item>
        <List.Item>Hovmestere, servitører og hjelpepersonell</List.Item>
        <List.Item>Mellomledere innen reiseliv og transport</List.Item>
      </List>
      <List as="ul" size="small" title="Serviceyrker og annet arbeid">
        <List.Item>Yrker innen politi, brannvesen, toll og forsvar</List.Item>
        <List.Item>Velvære</List.Item>
        <List.Item>Rengjøring</List.Item>
        <List.Item>Vakthold og vaktmestere</List.Item>
        <List.Item>Annet arbeid</List.Item>
        <List.Item>Yrker innen kunst, sport og kultur</List.Item>
      </List>
    </div>
  );
}
