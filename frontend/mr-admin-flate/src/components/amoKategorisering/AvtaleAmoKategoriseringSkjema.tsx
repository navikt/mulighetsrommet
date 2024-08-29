import { HGrid, Select } from "@navikt/ds-react";
import { Kurstype } from "@mr/api-client";
import { useFormContext } from "react-hook-form";
import { kurstypeToString } from "../../utils/Utils";
import { InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";
import { AvtaleBransjeSkjema } from "./AvtaleBransjeSkjema";
import { NorksopplaeringSkjema } from "./NorskopplaeringSkjema";
import { InnholdElementerSkjema } from "./InnholdElementerSkjema";

export function AvtaleAmoKategoriseringSkjema() {
  const {
    setValue,
    watch,
    formState: { errors },
  } = useFormContext<InferredAvtaleSchema>();

  const amoKategorisering = watch("amoKategorisering");

  return (
    <HGrid gap="4" columns={1}>
      <Select
        size="small"
        label={tiltaktekster.kurstypeLabel}
        value={amoKategorisering?.kurstype}
        error={errors?.amoKategorisering?.kurstype?.message}
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
      {amoKategorisering?.kurstype === Kurstype.BRANSJE_OG_YRKESRETTET && <AvtaleBransjeSkjema />}
      {amoKategorisering?.kurstype === Kurstype.NORSKOPPLAERING && (
        <NorksopplaeringSkjema<InferredAvtaleSchema>
          norskprovePath="amoKategorisering.norskprove"
          innholdElementerPath="amoKategorisering.innholdElementer"
        />
      )}
      {amoKategorisering?.kurstype === Kurstype.GRUNNLEGGENDE_FERDIGHETER && (
        <InnholdElementerSkjema<InferredAvtaleSchema> path="amoKategorisering.innholdElementer" />
      )}
    </HGrid>
  );
}

/*
 */
