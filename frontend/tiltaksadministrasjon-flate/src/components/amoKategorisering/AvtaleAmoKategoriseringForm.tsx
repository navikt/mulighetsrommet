import { HGrid } from "@navikt/ds-react";
import { useEffect, useRef } from "react";
import { useFormContext } from "react-hook-form";
import { kurstypeToString } from "@/utils/Utils";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { AvtaleBransjeForm } from "./AvtaleBransjeForm";
import { NorksopplaeringForm } from "./NorskopplaeringForm";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { AvtaleFormValues } from "@/pages/avtaler/form/validation";
import { KurstypeKode, Tiltakskode } from "@tiltaksadministrasjon/api-client";
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
  const { watch, setValue } = useFormContext<AvtaleFormValues>();

  const amoKategorisering = watch("detaljer.amoKategorisering");
  const prevKurstype = useRef(amoKategorisering?.kurstype);

  useEffect(() => {
    if (prevKurstype.current === amoKategorisering?.kurstype) return;
    prevKurstype.current = amoKategorisering?.kurstype;
    setValue("detaljer.amoKategorisering.norskprove", undefined);
  }, [amoKategorisering?.kurstype]);

  return (
    <HGrid gap="space-16" columns={1}>
      <FormSelect<AvtaleFormValues>
        name="detaljer.amoKategorisering.kurstype"
        label={gjennomforingTekster.kurstypeLabel}
      >
        <option value="">Velg kurstype</option>
        <option value={KurstypeKode.NORSKOPPLAERING}>
          {kurstypeToString(KurstypeKode.NORSKOPPLAERING)}
        </option>
        <option value={KurstypeKode.GRUNNLEGGENDE_FERDIGHETER}>
          {kurstypeToString(KurstypeKode.GRUNNLEGGENDE_FERDIGHETER)}
        </option>
        <option value={KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE}>
          {kurstypeToString(KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE)}
        </option>
      </FormSelect>
      {amoKategorisering?.kurstype === KurstypeKode.NORSKOPPLAERING && (
        <NorksopplaeringForm<AvtaleFormValues>
          norskprovePath="detaljer.amoKategorisering.norskprove"
          innholdElementerPath="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV}
        />
      )}
      {isGrunnelggendeFerdigheterOrFov(amoKategorisering?.kurstype) && (
        <InnholdElementerForm<AvtaleFormValues>
          path="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV}
        />
      )}
    </HGrid>
  );
}

function GruppeAmoForm() {
  const { watch, setValue } = useFormContext<AvtaleFormValues>();

  const amoKategorisering = watch("detaljer.amoKategorisering");
  const prevKurstype = useRef(amoKategorisering?.kurstype);

  useEffect(() => {
    if (prevKurstype.current === amoKategorisering?.kurstype) return;
    prevKurstype.current = amoKategorisering?.kurstype;
    setValue("detaljer.amoKategorisering.bransje", undefined);
    setValue("detaljer.amoKategorisering.forerkort", undefined);
    setValue("detaljer.amoKategorisering.sertifiseringer", undefined);
    setValue("detaljer.amoKategorisering.innholdElementer", undefined);
    setValue("detaljer.amoKategorisering.norskprove", undefined);
  }, [amoKategorisering?.kurstype]);

  return (
    <HGrid gap="space-16" columns={1}>
      <FormSelect<AvtaleFormValues>
        name="detaljer.amoKategorisering.kurstype"
        label={gjennomforingTekster.kurstypeLabel}
      >
        <option value="">Velg kurstype</option>
        <option value={KurstypeKode.BRANSJE_OG_YRKESRETTET}>
          {kurstypeToString(KurstypeKode.BRANSJE_OG_YRKESRETTET)}
        </option>
        <option value={KurstypeKode.NORSKOPPLAERING}>
          {kurstypeToString(KurstypeKode.NORSKOPPLAERING)}
        </option>
        <option value={KurstypeKode.GRUNNLEGGENDE_FERDIGHETER}>
          {kurstypeToString(KurstypeKode.GRUNNLEGGENDE_FERDIGHETER)}
        </option>
        <option value={KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE}>
          {kurstypeToString(KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE)}
        </option>
        <option value={KurstypeKode.STUDIESPESIALISERING}>
          {kurstypeToString(KurstypeKode.STUDIESPESIALISERING)}
        </option>
      </FormSelect>
      {amoKategorisering?.kurstype === KurstypeKode.BRANSJE_OG_YRKESRETTET && (
        <AvtaleBransjeForm tiltakskode={Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING} />
      )}
      {amoKategorisering?.kurstype === KurstypeKode.NORSKOPPLAERING && (
        <NorksopplaeringForm<AvtaleFormValues>
          norskprovePath="detaljer.amoKategorisering.norskprove"
          innholdElementerPath="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING}
        />
      )}
      {isGrunnelggendeFerdigheterOrFov(amoKategorisering?.kurstype) && (
        <InnholdElementerForm<AvtaleFormValues>
          path="detaljer.amoKategorisering.innholdElementer"
          tiltakskode={Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING}
        />
      )}
    </HGrid>
  );
}

function isGrunnelggendeFerdigheterOrFov(kurstype: KurstypeKode | null | undefined): boolean {
  if (!kurstype) {
    return false;
  }
  return [
    KurstypeKode.GRUNNLEGGENDE_FERDIGHETER,
    KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
  ].includes(kurstype);
}
