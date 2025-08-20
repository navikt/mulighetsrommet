import { HGrid, Select } from "@navikt/ds-react";
import { Kurstype } from "@mr/api-client-v2";
import { useFormContext } from "react-hook-form";
import { kurstypeToString } from "@/utils/Utils";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { AvtaleBransjeForm } from "./AvtaleBransjeForm";
import { NorksopplaeringForm } from "./NorskopplaeringForm";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { AvtaleFormValues } from "@/schemas/avtale";

export function AvtaleAmoKategoriseringForm() {
  const {
    setValue,
    watch,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();

  const amoKategorisering = watch("amoKategorisering");

  return (
    <HGrid gap="4" columns={1}>
      <Select
        size="small"
        label={gjennomforingTekster.kurstypeLabel}
        value={amoKategorisering?.kurstype}
        error={errors.amoKategorisering?.kurstype?.message}
        onChange={(type) => {
          setValue("amoKategorisering.kurstype", type.target.value as Kurstype);
        }}
      >
        <option value={undefined}>Velg kurstype</option>
        <option value={Kurstype.BRANSJE_OG_YRKESRETTET}>
          {kurstypeToString(Kurstype.BRANSJE_OG_YRKESRETTET)}
        </option>
        <option value={Kurstype.NORSKOPPLAERING}>
          {kurstypeToString(Kurstype.NORSKOPPLAERING)}
        </option>
        <option value={Kurstype.GRUNNLEGGENDE_FERDIGHETER}>
          {kurstypeToString(Kurstype.GRUNNLEGGENDE_FERDIGHETER)}
        </option>
        <option value={Kurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE}>
          {kurstypeToString(Kurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE)}
        </option>
        <option value={Kurstype.STUDIESPESIALISERING}>
          {kurstypeToString(Kurstype.STUDIESPESIALISERING)}
        </option>
      </Select>
      {amoKategorisering?.kurstype === Kurstype.BRANSJE_OG_YRKESRETTET && <AvtaleBransjeForm />}
      {amoKategorisering?.kurstype === Kurstype.NORSKOPPLAERING && (
        <NorksopplaeringForm<AvtaleFormValues>
          norskprovePath="amoKategorisering.norskprove"
          innholdElementerPath="amoKategorisering.innholdElementer"
        />
      )}
      {amoKategorisering?.kurstype === Kurstype.GRUNNLEGGENDE_FERDIGHETER && (
        <InnholdElementerForm<AvtaleFormValues> path="amoKategorisering.innholdElementer" />
      )}
    </HGrid>
  );
}
