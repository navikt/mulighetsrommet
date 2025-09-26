import { Box, Select, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { Prismodell, Tiltakskode } from "@mr/api-client-v2";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { AvtaleFormValues } from "@/schemas/avtale";
import { usePrismodeller } from "@/api/avtaler/usePrismodeller";
import PrismodellForm from "./PrismodellForm";
import { yyyyMMddFormatting } from "@mr/frontend-common/utils/date";

interface Props {
  tiltakskode: Tiltakskode;
  avtaleStartDato?: Date;
}

export default function AvtalePrismodellForm({ tiltakskode, avtaleStartDato }: Props) {
  const {
    formState: { errors },
    setValue,
    watch,
  } = useFormContext<AvtaleFormValues>();
  const { data: prismodeller = [] } = usePrismodeller(tiltakskode);

  const type = watch("prismodell.type");
  const satser = watch("prismodell.satser");

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
          readOnly={type === Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK}
          label={avtaletekster.prismodell.label}
          size="small"
          error={errors.prismodell?.type?.message}
          value={type}
          onChange={(e) => {
            setValue("prismodell.type", e.target.value as Prismodell);
            if (erPrismodellMedAvtalteSatser(e.target.value as Prismodell)) {
              if (satser.length === 0) {
                setValue("prismodell.satser", [
                  {
                    gjelderFra: yyyyMMddFormatting(avtaleStartDato) ?? null,
                    pris: 0,
                    valuta: "NOK",
                  },
                ]);
              }
            } else {
              setValue("prismodell.satser", []);
            }
          }}
        >
          <option key={undefined} value={undefined}></option>
          {prismodeller.map(({ type, beskrivelse }) => (
            <option key={type} value={type}>
              {beskrivelse}
            </option>
          ))}
        </Select>
        {avtaleStartDato && (
          <PrismodellForm
            prismodell={type}
            tiltakskode={tiltakskode}
            avtaleStartDato={avtaleStartDato}
          />
        )}
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
