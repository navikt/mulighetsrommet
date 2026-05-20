import { AvtaleDto, KurstypeKode } from "@tiltaksadministrasjon/api-client";
import { Alert, HGrid } from "@navikt/ds-react";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { kreverAmo } from "@/utils/tiltakstype";
import { OpplaringKategoriseringForm } from "./OpplaringKategoriseringForm";

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
      <OpplaringKategoriseringForm tiltakskode={avtale.tiltakstype.tiltakskode} />
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
