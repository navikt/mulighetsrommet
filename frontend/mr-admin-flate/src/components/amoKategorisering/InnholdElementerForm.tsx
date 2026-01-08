import { Checkbox, CheckboxGroup, HGrid } from "@navikt/ds-react";
import { Controller, FieldValues, Path, useFormContext } from "react-hook-form";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import {
  AmoKategoriseringInnholdElement as InnholdElement,
  Tiltakskode,
} from "@tiltaksadministrasjon/api-client";
import { innholdElementToString } from "@/utils/Utils";

interface Props<T> {
  path: Path<T>;
  tiltakskode: Tiltakskode;
}

export function InnholdElementerForm<T extends FieldValues>({ path, tiltakskode }: Props<T>) {
  const { control } = useFormContext<T>();

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
    <Controller
      name={path}
      control={control}
      render={({ field, fieldState: { error } }) => (
        <CheckboxGroup
          size="small"
          legend={gjennomforingTekster.innholdElementerLabel}
          onChange={(value) => field.onChange(value)}
          error={error?.message}
          value={field.value ?? []}
        >
          <HGrid columns={2}>
            {elementer().map((e: InnholdElement) => (
              <Checkbox value={e}>{innholdElementToString(e)}</Checkbox>
            ))}
          </HGrid>
        </CheckboxGroup>
      )}
    />
  );
}
