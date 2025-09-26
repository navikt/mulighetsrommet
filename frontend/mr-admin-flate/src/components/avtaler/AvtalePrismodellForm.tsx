import { Box, Select, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { PrismodellType, Tiltakskode } from "@mr/api-client-v2";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { PrismodellValues } from "@/schemas/avtale";
import { usePrismodeller } from "@/api/avtaler/usePrismodeller";
import PrismodellForm from "./PrismodellForm";
import { yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { FeatureToggle } from "@tiltaksadministrasjon/api-client";

interface Props {
  tiltakskode: Tiltakskode;
  avtaleStartDato?: Date;
}

export default function AvtalePrismodellForm({ tiltakskode, avtaleStartDato }: Props) {
  const {
    formState: { errors },
    setValue,
    watch,
  } = useFormContext<PrismodellValues>();
  const { data: prismodeller = [] } = usePrismodeller(tiltakskode);
  const { data: enabledHeleUker } = useFeatureToggle(
    FeatureToggle.MULIGHETSROMMET_PRISMODELL_HELE_UKER,
  );

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
          readOnly={prismodell === PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK}
          label={avtaletekster.prismodell.label}
          size="small"
          error={errors.prismodell?.message}
          value={prismodell}
          onChange={(e) => {
            const type = e.target.value as PrismodellType;
            setValue("prismodell", type);
            if (erPrismodellMedAvtalteSatser(type)) {
              if (satser.length === 0) {
                setValue("satser", [
                  {
                    gjelderFra: yyyyMMddFormatting(avtaleStartDato) ?? null,
                    gjelderTil: null,
                    pris: 0,
                    valuta: "NOK",
                  },
                ]);
              }
            } else {
              setValue("satser", []);
            }
          }}
        >
          <option key={undefined} value={undefined}></option>
          {prismodeller
            .filter(
              ({ type }) =>
                type !== PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK || enabledHeleUker,
            )
            .map(({ type, beskrivelse }) => (
              <option key={type} value={type}>
                {beskrivelse}
              </option>
            ))}
        </Select>
        {avtaleStartDato && (
          <PrismodellForm prismodell={prismodell} avtaleStartDato={avtaleStartDato} />
        )}
      </VStack>
    </Box>
  );
}

function erPrismodellMedAvtalteSatser(prismodell: PrismodellType) {
  return [
    PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
    PrismodellType.AVTALT_PRIS_PER_UKESVERK,
    PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
  ].includes(prismodell);
}
