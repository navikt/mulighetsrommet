import { ControlledSokeSelect } from "@mr/frontend-common";
import { GjennomforingOppstartstype } from "@tiltaksadministrasjon/api-client";
import { useController } from "react-hook-form";

interface SelectOppstartstypeProps {
  name: string;
  readonly: boolean;
}

export function SelectOppstartstype({ name, readonly = false }: SelectOppstartstypeProps) {
  const { field } = useController({ name });

  return (
    <ControlledSokeSelect
      size="small"
      label="Oppstartstype"
      placeholder="Velg oppstart"
      name={name}
      onChange={field.onChange}
      readOnly={readonly}
      options={[
        {
          label: "Felles oppstartsdato",
          value: GjennomforingOppstartstype.FELLES,
        },
        {
          label: "Løpende oppstart",
          value: GjennomforingOppstartstype.LOPENDE,
        },
      ]}
    />
  );
}
