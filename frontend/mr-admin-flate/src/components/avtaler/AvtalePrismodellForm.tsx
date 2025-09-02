import { Box, Select, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { Prismodell, Tiltakskode } from "@mr/api-client-v2";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { PrismodellValues } from "@/schemas/avtale";
import { usePrismodeller } from "@/api/tilsagn/usePrismodeller";
import PrismodellForm from "./PrismodellForm";

interface Props {
  tiltakskode: Tiltakskode;
}

export default function AvtalePrismodellForm({ tiltakskode }: Props) {
  const {
    register,
    formState: { errors },
    setValue,
    watch,
  } = useFormContext<PrismodellValues>();

  const { data: prismodeller = [] } = usePrismodeller(tiltakskode);

  const prismodell = watch("prismodell");
  const satser = watch("satser");

  return (
    <Box
      borderWidth="1"
      borderColor="border-subtle"
      borderRadius="large"
      padding="4"
      background="surface-subtle"
    >
      <VStack gap="2">
        <Select
          readOnly={prismodell === Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK}
          label={avtaletekster.prismodell.label}
          size="small"
          error={errors.prismodell?.message}
          {...register("prismodell", {
            onChange: (e) => {
              if (
                erPrismodellMedAvtalteSatser(e.target.value as Prismodell) &&
                satser.length === 0
              ) {
                setValue("satser", [
                  {
                    periodeStart: "",
                    periodeSlutt: "",
                    pris: 0,
                    valuta: "NOK",
                  },
                ]);
              } else {
                setValue("satser", []);
              }
            },
          })}
        >
          <option key={undefined} value={undefined}></option>
          {prismodeller.map(({ type, beskrivelse }) => (
            <option key={type} value={type}>
              {beskrivelse}
            </option>
          ))}
        </Select>
        <PrismodellForm prismodell={prismodell} tiltakskode={tiltakskode} />
      </VStack>
    </Box>
  );
}

function erPrismodellMedAvtalteSatser(prismodell: Prismodell) {
  return [
    Prismodell.AVTALT_PRIS_PER_MANEDSVERK,
    Prismodell.AVTALT_PRIS_PER_UKESVERK,
    Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
  ].includes(prismodell);
}
