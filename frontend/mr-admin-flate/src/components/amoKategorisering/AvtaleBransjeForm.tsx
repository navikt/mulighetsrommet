import { Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { AvtaleFormValues } from "@/pages/avtaler/form/validation";
import { OpplaringKategoriseringForm } from "./OpplaringKategoriseringForm";

interface Props {
  tiltakskode: Tiltakskode;
}

export function AvtaleBransjeForm({ tiltakskode }: Props) {
  return (
    <>
      <OpplaringKategoriseringForm tiltakskode={tiltakskode} />
      <InnholdElementerForm<AvtaleFormValues>
        tiltakskode={tiltakskode}
        path={"detaljer.amoKategorisering.innholdElementer"}
      />
    </>
  );
}
