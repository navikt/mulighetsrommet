import { HGrid } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { AvtaleBransjeForm } from "./AvtaleBransjeForm";
import { NorksopplaeringForm } from "./NorskopplaeringForm";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { AvtaleFormValues } from "@/pages/avtaler/form/validation";
import { KurstypeKode, Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { OpplaringKategoriseringForm } from "./OpplaringKategoriseringForm";

interface Props {
  tiltakskode: Tiltakskode;
}

export function AvtaleAmoKategoriseringForm({ tiltakskode }: Props) {
  if (tiltakskode === Tiltakskode.ARBEIDSMARKEDSOPPLAERING) {
    return <AvtaleBransjeForm tiltakskode={Tiltakskode.ARBEIDSMARKEDSOPPLAERING} />;
  } else if (tiltakskode === Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV) {
    return (
      <NorskopplaeringGrunnleggendeGerdigheterFOVForm
        tiltakskode={Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV}
      />
    );
  } else if (tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING) {
    return <GruppeAmoForm tiltakskode={tiltakskode} />;
  } else {
    return null;
  }
}

function NorskopplaeringGrunnleggendeGerdigheterFOVForm({
  tiltakskode,
}: {
  tiltakskode: Tiltakskode;
}) {
  const { watch } = useFormContext<AvtaleFormValues>();

  const amoKategorisering = watch("detaljer.amoKategorisering");

  return (
    <HGrid gap="space-16" columns={1}>
      <OpplaringKategoriseringForm tiltakskode={tiltakskode} />
      {amoKategorisering?.kurstypeId === KurstypeKode.NORSKOPPLAERING && (
        <NorksopplaeringForm<AvtaleFormValues>
          norskprovePath="detaljer.amoKategorisering.norskprove"
          innholdElementerPath="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV}
        />
      )}
      {(amoKategorisering?.kurstypeId === KurstypeKode.GRUNNLEGGENDE_FERDIGHETER ||
        amoKategorisering?.kurstypeId === KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE) && (
        <InnholdElementerForm<AvtaleFormValues>
          path="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV}
        />
      )}
    </HGrid>
  );
}

function GruppeAmoForm({ tiltakskode }: { tiltakskode: Tiltakskode }) {
  const { watch } = useFormContext<AvtaleFormValues>();

  const amoKategorisering = watch("detaljer.amoKategorisering");

  return (
    <HGrid gap="space-16" columns={1}>
      <OpplaringKategoriseringForm tiltakskode={tiltakskode} />
      {amoKategorisering?.kurstypeId === KurstypeKode.NORSKOPPLAERING && (
        <NorksopplaeringForm<AvtaleFormValues>
          norskprovePath="detaljer.amoKategorisering.norskprove"
          innholdElementerPath="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING}
        />
      )}
      {amoKategorisering?.kurstypeId === KurstypeKode.GRUNNLEGGENDE_FERDIGHETER && (
        <InnholdElementerForm<AvtaleFormValues>
          path="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING}
        />
      )}
    </HGrid>
  );
}
