import { Checkbox, HGrid } from "@navikt/ds-react";
import { FieldValues, Path } from "react-hook-form";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import {
  AmoKategoriseringInnholdElement as InnholdElement,
  Tiltakskode,
} from "@tiltaksadministrasjon/api-client";
import { innholdElementToString } from "@/utils/Utils";
import { FormCheckboxGroup } from "@/components/skjema/FormCheckboxGroup";

interface Props<T> {
  path: Path<T>;
  tiltakskode: Tiltakskode;
}

export function InnholdElementerForm<T extends FieldValues>({ path, tiltakskode }: Props<T>) {
  function elementer() {
    if (tiltakskode === Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV) {
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
