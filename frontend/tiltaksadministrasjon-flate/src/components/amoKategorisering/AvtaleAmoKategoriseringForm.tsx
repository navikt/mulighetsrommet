import { HGrid } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { kurstypeToString } from "@/utils/Utils";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { AvtaleBransjeForm } from "./AvtaleBransjeForm";
import { NorksopplaeringForm } from "./NorskopplaeringForm";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { AvtaleFormValues } from "@/pages/avtaler/form/validation";
import { AmoKurstype, Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { FormSelect } from "@/components/skjema/FormSelect";

interface Props {
  tiltakskode: Tiltakskode;
}

export function AvtaleAmoKategoriseringForm({ tiltakskode }: Props) {
  if (tiltakskode === Tiltakskode.ARBEIDSMARKEDSOPPLAERING) {
    return <AvtaleBransjeForm tiltakskode={Tiltakskode.ARBEIDSMARKEDSOPPLAERING} />;
  } else if (tiltakskode === Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV) {
    return <NorskopplaeringGrunnleggendeGerdigheterFOVForm />;
  } else if (tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING) {
    return <GruppeAmoForm />;
  } else {
    return null;
  }
}

function NorskopplaeringGrunnleggendeGerdigheterFOVForm() {
  const { watch } = useFormContext<AvtaleFormValues>();

  const amoKategorisering = watch("detaljer.amoKategorisering");

  return (
    <HGrid gap="space-16" columns={1}>
      <FormSelect<AvtaleFormValues>
        name="detaljer.amoKategorisering.kurstype"
        label={gjennomforingTekster.kurstypeLabel}
      >
        <option value="">Velg kurstype</option>
        <option value={AmoKurstype.NORSKOPPLAERING}>
          {kurstypeToString(AmoKurstype.NORSKOPPLAERING)}
        </option>
        <option value={AmoKurstype.GRUNNLEGGENDE_FERDIGHETER}>
          {kurstypeToString(AmoKurstype.GRUNNLEGGENDE_FERDIGHETER)}
        </option>
        <option value={AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE}>
          {kurstypeToString(AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE)}
        </option>
      </FormSelect>
      {amoKategorisering?.kurstype === AmoKurstype.NORSKOPPLAERING && (
        <NorksopplaeringForm<AvtaleFormValues>
          norskprovePath="detaljer.amoKategorisering.norskprove"
          innholdElementerPath="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV}
        />
      )}
      {(amoKategorisering?.kurstype === AmoKurstype.GRUNNLEGGENDE_FERDIGHETER ||
        amoKategorisering?.kurstype === AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE) && (
        <InnholdElementerForm<AvtaleFormValues>
          path="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV}
        />
      )}
    </HGrid>
  );
}

function GruppeAmoForm() {
  const { watch } = useFormContext<AvtaleFormValues>();

  const amoKategorisering = watch("detaljer.amoKategorisering");

  return (
    <HGrid gap="space-16" columns={1}>
      <FormSelect<AvtaleFormValues>
        name="detaljer.amoKategorisering.kurstype"
        label={gjennomforingTekster.kurstypeLabel}
      >
        <option value="">Velg kurstype</option>
        <option value={AmoKurstype.BRANSJE_OG_YRKESRETTET}>
          {kurstypeToString(AmoKurstype.BRANSJE_OG_YRKESRETTET)}
        </option>
        <option value={AmoKurstype.NORSKOPPLAERING}>
          {kurstypeToString(AmoKurstype.NORSKOPPLAERING)}
        </option>
        <option value={AmoKurstype.GRUNNLEGGENDE_FERDIGHETER}>
          {kurstypeToString(AmoKurstype.GRUNNLEGGENDE_FERDIGHETER)}
        </option>
        <option value={AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE}>
          {kurstypeToString(AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE)}
        </option>
        <option value={AmoKurstype.STUDIESPESIALISERING}>
          {kurstypeToString(AmoKurstype.STUDIESPESIALISERING)}
        </option>
      </FormSelect>
      {amoKategorisering?.kurstype === AmoKurstype.BRANSJE_OG_YRKESRETTET && (
        <AvtaleBransjeForm tiltakskode={Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING} />
      )}
      {amoKategorisering?.kurstype === AmoKurstype.NORSKOPPLAERING && (
        <NorksopplaeringForm<AvtaleFormValues>
          norskprovePath="detaljer.amoKategorisering.norskprove"
          innholdElementerPath="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING}
        />
      )}
      {amoKategorisering?.kurstype === AmoKurstype.GRUNNLEGGENDE_FERDIGHETER && (
        <InnholdElementerForm<AvtaleFormValues>
          path="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING}
        />
      )}
    </HGrid>
  );
}
