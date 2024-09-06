import { InferredTiltaksgjennomforingSchema } from "@/components/redaksjoneltInnhold/TiltaksgjennomforingSchema";
import { bransjeToString, kurstypeToString } from "@/utils/Utils";
import { Avtale, Kurstype } from "@mr/api-client";
import { HGrid, Select } from "@navikt/ds-react";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";
import { ForerkortSkjema } from "./ForerkortSkjema";
import { InnholdElementerSkjema } from "./InnholdElementerSkjema";
import { NorksopplaeringSkjema } from "./NorskopplaeringSkjema";
import { SertifiseringerSkjema } from "./SertifiseringerSelect";

interface Props {
  avtale: Avtale;
}

export function TiltaksgjennomforingAmoKategoriseringSkjema(props: Props) {
  const { avtale } = props;

  if (!avtale.amoKategorisering) {
    return null;
  }

  const avtaleAmo = avtale.amoKategorisering;

  return (
    <HGrid gap="4" columns={1}>
      <Select readOnly size="small" label={tiltaktekster.kurstypeLabel}>
        <option>{kurstypeToString(avtaleAmo.kurstype as Kurstype)}</option>
      </Select>
      {avtaleAmo.kurstype == Kurstype.BRANSJE_OG_YRKESRETTET && (
        <>
          <Select readOnly size="small" label="Bransje">
            <option>{bransjeToString(avtaleAmo.bransje)}</option>
          </Select>
          {avtaleAmo.forerkort && avtaleAmo.forerkort.length > 0 && (
            <ForerkortSkjema<InferredTiltaksgjennomforingSchema>
              path="amoKategorisering.forerkort"
              options={avtaleAmo.forerkort}
            />
          )}
          {avtaleAmo.sertifiseringer && avtaleAmo.sertifiseringer.length > 0 && (
            <SertifiseringerSkjema<InferredTiltaksgjennomforingSchema>
              path="amoKategorisering.sertifiseringer"
              options={avtaleAmo.sertifiseringer}
            />
          )}
          <InnholdElementerSkjema<InferredTiltaksgjennomforingSchema> path="amoKategorisering.innholdElementer" />
        </>
      )}
      {avtaleAmo.kurstype == Kurstype.NORSKOPPLAERING && (
        <NorksopplaeringSkjema<InferredTiltaksgjennomforingSchema>
          norskprovePath="amoKategorisering.norskprove"
          innholdElementerPath="amoKategorisering.innholdElementer"
        />
      )}
    </HGrid>
  );
}
