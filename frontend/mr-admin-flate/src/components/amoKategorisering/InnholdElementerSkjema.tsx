import { Checkbox, CheckboxGroup, HGrid } from "@navikt/ds-react";
import { Controller, FieldValues, Path, useFormContext } from "react-hook-form";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";
import { InnholdElement } from "@mr/api-client";
import { innholdElementToString } from "@/utils/Utils";

export function InnholdElementerSkjema<T extends FieldValues>(props: { path: Path<T> }) {
  const { path } = props;
  const { control } = useFormContext<T>();

  return (
    <Controller
      name={path}
      control={control}
      render={({ field, fieldState: { error } }) => (
        <CheckboxGroup
          size="small"
          legend={tiltaktekster.innholdElementerLabel}
          onChange={(value) => field.onChange(value)}
          error={error?.message}
          value={field.value}
        >
          <HGrid columns={2}>
            <Checkbox value={InnholdElement.GRUNNLEGGENDE_FERDIGHETER}>
              {innholdElementToString(InnholdElement.GRUNNLEGGENDE_FERDIGHETER)}
            </Checkbox>
            <Checkbox value={InnholdElement.JOBBSOKER_KOMPETANSE}>
              {innholdElementToString(InnholdElement.JOBBSOKER_KOMPETANSE)}
            </Checkbox>
            <Checkbox value={InnholdElement.TEORETISK_OPPLAERING}>
              {innholdElementToString(InnholdElement.TEORETISK_OPPLAERING)}
            </Checkbox>
            <Checkbox value={InnholdElement.PRAKSIS}>
              {innholdElementToString(InnholdElement.PRAKSIS)}
            </Checkbox>
            <Checkbox value={InnholdElement.ARBEIDSMARKEDSKUNNSKAP}>
              {innholdElementToString(InnholdElement.ARBEIDSMARKEDSKUNNSKAP)}
            </Checkbox>
            <Checkbox value={InnholdElement.NORSKOPPLAERING}>
              {innholdElementToString(InnholdElement.NORSKOPPLAERING)}
            </Checkbox>
          </HGrid>
        </CheckboxGroup>
      )}
    />
  );
}
