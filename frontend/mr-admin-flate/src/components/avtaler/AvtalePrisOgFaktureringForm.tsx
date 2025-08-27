import { Select } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { Prismodell, Tiltakskode } from "@mr/api-client-v2";
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
  const satser = watch("satser");

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
        {...register("prismodell", {
          onChange: (e) => {
            if (erPrismodellMedAvtalteSatser(e.target.value as Prismodell) && satser.length === 0) {
              setValue("satser", [
                {
                  periodeStart: "",
                  periodeSlutt: "",
                  pris: 0,
                  valuta: "NOK",
                },
              ]);
            }
          },
        })}
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

function erPrismodellMedAvtalteSatser(prismodell: Prismodell) {
  return [
    Prismodell.AVTALT_PRIS_PER_MANEDSVERK,
    Prismodell.AVTALT_PRIS_PER_UKESVERK,
    Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
  ].includes(prismodell);
}
