import { KurstypeKode, AvtaleDto } from "@tiltaksadministrasjon/api-client";
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

  if (!avtale.opplaring) {
    return <Alert variant="warning">{gjennomforingTekster.amoKategoriseringMangler}</Alert>;
  }

  const avtaleAmo = avtale.opplaring;

  return (
    <HGrid gap="space-16" columns={1}>
      {avtaleAmo.kurstype?.kode === KurstypeKode.BRANSJE_OG_YRKESRETTET && (
        <>
          <Select readOnly size="small" label="Bransje">
            <option>{avtaleAmo.bransje ? avtaleAmo.bransje.navn : "-"}</option>
          </Select>
          {avtaleAmo.forerkort.length > 0 && (
            <ForerkortForm
              path="amoKategorisering.forerkort"
              options={avtaleAmo.forerkort.map((f) => f.kode)}
            />
          )}
          {avtaleAmo.sertifiseringer.length > 0 && (
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
      {avtaleAmo.kurstype?.kode === KurstypeKode.NORSKOPPLAERING && (
        <NorksopplaeringForm
          norskprovePath="amoKategorisering.norskprove"
          innholdElementerPath="amoKategorisering.innholdElementer"
          tiltakskode={avtale.tiltakstype.tiltakskode}
        />
      )}
      {(avtaleAmo.kurstype?.kode === KurstypeKode.GRUNNLEGGENDE_FERDIGHETER ||
        avtaleAmo.kurstype?.kode === KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE) && (
        <InnholdElementerForm
          path="amoKategorisering.innholdElementer"
          tiltakskode={avtale.tiltakstype.tiltakskode}
        />
      )}
    </HGrid>
  );
}
