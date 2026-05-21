import { Checkbox, HGrid } from "@navikt/ds-react";
import { FieldValues, Path } from "react-hook-form";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import {
  AmoKategoriseringInnholdElement as InnholdElement,
  Kurstype,
  KurstypeKode,
  Tiltakskode,
} from "@tiltaksadministrasjon/api-client";
import { innholdElementToString } from "@/utils/Utils";
import { FormCheckboxGroup } from "@/components/skjema/FormCheckboxGroup";

interface Props<T> {
  path: Path<T>;
  tiltakskode: Tiltakskode;
  kurstype?: Kurstype;
}

const NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV_KURS = [
  KurstypeKode.NORSKOPPLAERING,
  KurstypeKode.GRUNNLEGGENDE_FERDIGHETER,
  KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
];
export function InnholdElementerForm<T extends FieldValues>({
  path,
  tiltakskode,
  kurstype,
}: Props<T>) {
  console.log({ kurstype: kurstype?.kode, tiltakskode });
  if (
    kurstype?.kode === KurstypeKode.STUDIESPESIALISERING ||
    tiltakskode === Tiltakskode.STUDIESPESIALISERING
  ) {
    return null;
  }

  function elementer() {
    if (
      tiltakskode === Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV ||
      (kurstype?.kode && NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV_KURS.includes(kurstype.kode))
    ) {
      return [
        InnholdElement.BRANSJERETTET_OPPLARING,
        InnholdElement.JOBBSOKER_KOMPETANSE,
        InnholdElement.PRAKSIS,
        InnholdElement.ARBEIDSMARKEDSKUNNSKAP,
      ];
    } else {
      return [
        InnholdElement.GRUNNLEGGENDE_FERDIGHETER,
        InnholdElement.JOBBSOKER_KOMPETANSE,
        InnholdElement.TEORETISK_OPPLAERING,
        InnholdElement.PRAKSIS,
        InnholdElement.ARBEIDSMARKEDSKUNNSKAP,
        InnholdElement.NORSKOPPLAERING,
      ];
    }
  }

  return (
    <FormCheckboxGroup<T>
      size="small"
      name={path}
      legend={gjennomforingTekster.innholdElementerLabel}
    >
      <HGrid columns={2}>
        {elementer().map((e: InnholdElement) => (
          <Checkbox key={e} value={e}>
            {innholdElementToString(e)}
          </Checkbox>
        ))}
      </HGrid>
    </FormCheckboxGroup>
  );
}
