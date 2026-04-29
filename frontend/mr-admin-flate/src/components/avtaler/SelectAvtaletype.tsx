import { AvtaleFormValues } from "@/pages/avtaler/form/validation";
import { useFormContext } from "react-hook-form";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { Avtaletype, Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { useAvtaletyper } from "@/api/avtaler/useAvtaletyper";
import { useEffect } from "react";
import { FormSelect } from "@/components/skjema/FormSelect";

interface SelectAvtaletypeProps {
  tiltakskode: Tiltakskode;
  readOnly: boolean;
  onChange: (avtaletype: Avtaletype) => void;
}

export function SelectAvtaletype({ tiltakskode, readOnly, onChange }: SelectAvtaletypeProps) {
  const { data: avtaletyper } = useAvtaletyper(tiltakskode);

  const { setValue, getValues } = useFormContext<AvtaleFormValues>();

  useEffect(() => {
    const current = getValues("detaljer.avtaletype");
    if (
      avtaletyper &&
      avtaletyper.length > 0 &&
      !avtaletyper.some((info) => info.type === current)
    ) {
      setValue("detaljer.avtaletype", avtaletyper[0].type);
      onChange(avtaletyper[0].type);
    }
  }, [tiltakskode, avtaletyper]);

  return (
    <FormSelect<AvtaleFormValues>
      name="detaljer.avtaletype"
      readOnly={readOnly}
      label={avtaletekster.avtaletypeLabel}
      rules={{ onChange: (e) => onChange(e.target.value) }}
    >
      {(avtaletyper ?? []).map((info) => (
        <option key={info.type} value={info.type}>
          {info.tittel}
        </option>
      ))}
    </FormSelect>
  );
}
