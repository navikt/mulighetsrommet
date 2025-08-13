import { Select } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { Tiltakskode } from "@mr/api-client-v2";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { AvtaleFormValues } from "@/schemas/avtale";
import { usePrismodeller } from "@/api/tilsagn/usePrismodeller";
import PrismodellForm from "./PrismodellForm";
import { useEffect } from "react";

interface AvtalPrisOgFaktureringProps {
  tiltakskode: Tiltakskode;
}

export default function AvtalePrisOgFaktureringForm({ tiltakskode }: AvtalPrisOgFaktureringProps) {
  const { data: prismodeller = [] } = usePrismodeller(tiltakskode);

  const {
    register,
    formState: { errors },
    setValue,
    watch,
  } = useFormContext<AvtaleFormValues>();

  const prismodell = watch("prismodell");

  useEffect(() => {
    if (prismodeller.some((p) => p.type === prismodell)) return;

    const newValue = prismodeller.length === 1 ? prismodeller[0].type : undefined;

    if (newValue !== prismodell) {
      setValue("prismodell", newValue, { shouldValidate: false });
    }
  }, [tiltakskode, prismodeller, prismodell, setValue]);

  return (
    <>
      <Select
        label={avtaletekster.prismodell.label}
        size="small"
        error={errors.prismodell?.message}
        {...register("prismodell")}
      >
        <option key="" value={undefined}>
          -- Velg en --
        </option>
        {prismodeller.map(({ type, beskrivelse }) => (
          <option key={type} value={type}>
            {beskrivelse}
          </option>
        ))}
      </Select>

      <PrismodellForm prismodell={prismodell} tiltakskode={tiltakskode} />
    </>
  );
}
