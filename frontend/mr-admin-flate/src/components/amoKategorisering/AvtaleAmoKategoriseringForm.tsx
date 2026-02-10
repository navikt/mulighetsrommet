import { HGrid, Select } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { kurstypeToString } from "@/utils/Utils";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { AvtaleBransjeForm } from "./AvtaleBransjeForm";
import { NorksopplaeringForm } from "./NorskopplaeringForm";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { AvtaleFormValues } from "@/schemas/avtale";
import { AmoKurstype, Tiltakskode } from "@tiltaksadministrasjon/api-client";

interface Props {
  tiltakskode: Tiltakskode;
}

export function AvtaleAmoKategoriseringForm({ tiltakskode }: Props) {
  switch (tiltakskode) {
    case Tiltakskode.ARBEIDSFORBEREDENDE_TRENING:
    case Tiltakskode.ARBEIDSRETTET_REHABILITERING:
    case Tiltakskode.AVKLARING:
    case Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK:
    case Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING:
    case Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING:
    case Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING:
    case Tiltakskode.HOYERE_UTDANNING:
    case Tiltakskode.JOBBKLUBB:
    case Tiltakskode.OPPFOLGING:
    case Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET:
    case Tiltakskode.FAG_OG_YRKESOPPLAERING:
    case Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING:
    case Tiltakskode.STUDIESPESIALISERING:
      return null;
    case Tiltakskode.ARBEIDSMARKEDSOPPLAERING:
      return <AvtaleBransjeForm tiltakskode={Tiltakskode.ARBEIDSMARKEDSOPPLAERING} />;
    case Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV:
      return <NorskopplaeringGrunnleggendeGerdigheterFOVForm />;
    case Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING:
      return <GruppeAmoForm />;
  }
}

function NorskopplaeringGrunnleggendeGerdigheterFOVForm() {
  const {
    setValue,
    watch,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();

  const amoKategorisering = watch("detaljer.amoKategorisering");

  return (
    <HGrid gap="space-16" columns={1}>
      <Select
        size="small"
        label={gjennomforingTekster.kurstypeLabel}
        value={amoKategorisering?.kurstype ?? undefined}
        error={errors.detaljer?.amoKategorisering?.kurstype?.message}
        onChange={(type) => {
          setValue(
            "detaljer.amoKategorisering.kurstype",
            (type.target.value || null) as AmoKurstype | null,
          );
        }}
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
      </Select>
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
  const {
    setValue,
    watch,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();

  const amoKategorisering = watch("detaljer.amoKategorisering");

  return (
    <HGrid gap="space-16" columns={1}>
      <Select
        size="small"
        label={gjennomforingTekster.kurstypeLabel}
        value={amoKategorisering?.kurstype ?? undefined}
        error={errors.detaljer?.amoKategorisering?.kurstype?.message}
        onChange={(type) => {
          setValue(
            "detaljer.amoKategorisering.kurstype",
            (type.target.value || null) as AmoKurstype | null,
          );
        }}
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
      </Select>
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
