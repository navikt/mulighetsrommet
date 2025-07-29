import { Select } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { Tiltakskode } from "@mr/api-client-v2";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { AvtaleFormValues } from "@/schemas/avtale";
import { usePrismodeller } from "@/api/tilsagn/usePrismodeller";
import PrismodellForm from "./PrismodellForm";
import { useMemo, useCallback, useEffect } from "react";

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
  const preselectPrismodell = useMemo(() => {
    return prismodeller.length === 1 ? prismodeller[0].type : null;
  }, [prismodeller]);

  const handlePreselect = useCallback(() => {
    if (preselectPrismodell && !prismodell) {
      setValue("prismodell", preselectPrismodell, { shouldValidate: false });
    }
  }, [preselectPrismodell, prismodell, setValue]);

  useEffect(() => {
    handlePreselect();
  }, [handlePreselect]);

  return (
    <>
      <Select
        label={avtaletekster.prismodell.label}
        size="small"
        error={errors.prismodell?.message}
        {...register("prismodell")}
      >
        {prismodeller.map(({ type, beskrivelse }) => (
          <option key={type} value={type}>
            {beskrivelse}
          </option>
        ))}
      </Select>

      <PrismodellForm tiltakskode={tiltakskode} />
    </>
  );
}
