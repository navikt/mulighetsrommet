import { HGrid } from "@navikt/ds-react";
import { Path, useFormContext } from "react-hook-form";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { AvtaleFormValues } from "@/pages/avtaler/form/validation";
import { KurstypeKode, Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { OpplaringKategoriseringForm } from "./OpplaringKategoriseringForm";
import { useOpplaringKurstyper } from "@/api/amo/useOpplaringKurstyper";
import { FormCheckbox } from "../skjema/FormCheckbox";

interface Props {
  tiltakskode: Tiltakskode;
}

export function AvtaleAmoKategoriseringForm({ tiltakskode }: Props) {
  const { data: kurstyper } = useOpplaringKurstyper();
  const { watch } = useFormContext<AvtaleFormValues>();

  const basePath = "detaljer.amoKategorisering" as Path<AvtaleFormValues>;
  const amoKategorisering = watch(basePath);
  const valgtKurstype = kurstyper.find((kurstype) => kurstype.id === amoKategorisering?.kurstypeId);

  return (
    <HGrid gap="space-16" columns={1}>
      <OpplaringKategoriseringForm tiltakskode={tiltakskode} basePath={basePath} />
      {valgtKurstype?.kode === KurstypeKode.NORSKOPPLAERING && (
        <FormCheckbox name={`${basePath}.norskprove`}>Gir mulighet for norskprøve</FormCheckbox>
      )}
      {harInnholdsElementer(tiltakskode) && (
        <InnholdElementerForm
          path={`${basePath}.innholdElementer`}
          tiltakskode={tiltakskode}
          kurstype={valgtKurstype}
        />
      )}
    </HGrid>
  );
}

function harInnholdsElementer(tiltakskode: Tiltakskode): boolean {
  return [
    Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
    Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
  ].includes(tiltakskode);
}
