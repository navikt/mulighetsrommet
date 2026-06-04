import { Alert, Select } from "@navikt/ds-react";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { FormComboboxMulti } from "@/components/skjema/FormComboboxMulti";
import { AvtaleDto } from "@tiltaksadministrasjon/api-client";
import { kreverUtdanningslop } from "@/utils/tiltakstype";
import { GjennomforingFormValues } from "@/pages/gjennomforing/form/validation";

interface Props {
  avtale: AvtaleDto;
}

export function GjennomforingUtdanningslopForm({ avtale }: Props) {
  if (!kreverUtdanningslop(avtale.tiltakstype.tiltakskode)) {
    return null;
  }

  if (!avtale.utdanningslop) {
    return (
      <Alert variant="warning">{avtaletekster.utdanning.utdanningsprogramManglerForAvtale}</Alert>
    );
  }

  return (
    <>
      <Select size="small" readOnly label={avtaletekster.utdanning.utdanningsprogram.label}>
        <option>{avtale.utdanningslop.utdanningsprogram.navn}</option>
      </Select>
      <FormComboboxMulti<GjennomforingFormValues>
        label={avtaletekster.utdanning.laerefag.label}
        placeholder={avtaletekster.utdanning.laerefag.velg}
        name="utdanningslop.utdanninger"
        options={avtale.utdanningslop.utdanninger.map((utdanning) => ({
          value: utdanning.id,
          label: utdanning.navn,
        }))}
      />
    </>
  );
}
