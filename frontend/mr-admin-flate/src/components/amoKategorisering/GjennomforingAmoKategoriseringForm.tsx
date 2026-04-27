import { bransjeToString } from "@/utils/Utils";
import { AmoKurstype, AvtaleDto } from "@tiltaksadministrasjon/api-client";
import { Alert, HGrid, Select } from "@navikt/ds-react";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { ForerkortForm } from "./ForerkortForm";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { NorksopplaeringForm } from "./NorskopplaeringForm";
import { SertifiseringerSkjema } from "./SertifiseringerSelect";
import { kreverAmo } from "@/utils/tiltakstype";

interface Props {
  avtale: AvtaleDto;
}

export function GjennomforingAmoKategoriseringForm({ avtale }: Props) {
  if (!kreverAmo(avtale.tiltakstype.tiltakskode)) {
    return null;
  }

  if (!avtale.amoKategorisering) {
    return <Alert variant="warning">{gjennomforingTekster.amoKategoriseringMangler}</Alert>;
  }

  const avtaleAmo = avtale.amoKategorisering;

  return (
    <HGrid gap="space-16" columns={1}>
      {avtaleAmo.kurstype === "BRANSJE_OG_YRKESRETTET" && (
        <>
          <Select readOnly size="small" label="Bransje">
            <option>{avtaleAmo.bransje ? bransjeToString(avtaleAmo.bransje) : "-"}</option>
          </Select>
          {avtaleAmo.forerkort && avtaleAmo.forerkort.length > 0 && (
            <ForerkortForm path="amoKategorisering.forerkort" options={avtaleAmo.forerkort} />
          )}
          {avtaleAmo.sertifiseringer && avtaleAmo.sertifiseringer.length > 0 && (
            <SertifiseringerSkjema
              path="amoKategorisering.sertifiseringer"
              options={avtaleAmo.sertifiseringer}
            />
          )}
          <InnholdElementerForm
            path="amoKategorisering.innholdElementer"
            tiltakskode={avtale.tiltakstype.tiltakskode}
          />
        </>
      )}
      {avtaleAmo.kurstype === "NORSKOPPLAERING" && (
        <NorksopplaeringForm
          norskprovePath="amoKategorisering.norskprove"
          innholdElementerPath="amoKategorisering.innholdElementer"
          tiltakskode={avtale.tiltakstype.tiltakskode}
        />
      )}
      {(avtaleAmo.kurstype === AmoKurstype.GRUNNLEGGENDE_FERDIGHETER ||
        avtaleAmo.kurstype === AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE) && (
        <InnholdElementerForm
          path="amoKategorisering.innholdElementer"
          tiltakskode={avtale.tiltakstype.tiltakskode}
        />
      )}
    </HGrid>
  );
}
