import { BodyShort, Box, Button, Select, VStack, HStack } from "@navikt/ds-react";
import { useFormContext, useFieldArray } from "react-hook-form";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { PrismodellValues } from "@/schemas/avtale";
import { usePrismodeller } from "@/api/avtaler/usePrismodeller";
import PrismodellForm from "./PrismodellForm";
import { yyyyMMddSafeFormatting } from "@mr/frontend-common/utils/date";
import { PrismodellType, Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";

interface Props {
  tiltakskode: Tiltakskode;
  avtaleStartDato: Date;
}

export default function AvtalePrismodellForm({ tiltakskode, avtaleStartDato }: Props) {
  const {
    formState: { errors },
    control,
    setValue,
    watch,
  } = useFormContext<PrismodellValues>();
  const { data: prismodellTyper = [] } = usePrismodeller(tiltakskode);

  const { fields, append, remove } = useFieldArray({
    name: "prismodeller",
    control,
  });

  return (
    <VStack gap="4">
      {fields.map((field, index) => {
        const type = watch(`prismodeller.${index}.type`);
        const satser = watch(`prismodeller.${index}.satser`);
        const beskrivelse = prismodellTyper.find((p) => p.type === type)?.beskrivelse;
        return (
          <Box
            key={field.id}
            borderWidth="1"
            borderColor="border-subtle"
            borderRadius="large"
            padding="4"
            background="surface-subtle"
          >
            <HStack justify="space-between" align="start">
              <VStack gap="4" style={{ flex: 1 }}>
                <Select
                  readOnly={type === PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK}
                  label={avtaletekster.prismodell.label}
                  size="small"
                  error={errors.prismodeller?.[index]?.type?.message}
                  value={type}
                  onChange={(e) => {
                    const nyType = e.target.value as PrismodellType;
                    setValue(`prismodeller.${index}.type`, nyType);
                    if (erPrismodellMedAvtalteSatser(nyType)) {
                      if (satser.length === 0) {
                        setValue(`prismodeller.${index}.satser`, [
                          {
                            gjelderFra: yyyyMMddSafeFormatting(avtaleStartDato),
                            gjelderTil: null,
                            pris: 0,
                            valuta: "NOK",
                          },
                        ]);
                      }
                    } else {
                      setValue(`prismodeller.${index}.satser`, []);
                    }
                  }}
                >
                  <option key={undefined} value={undefined}>
                    -- Velg prismodell --
                  </option>
                  {prismodellTyper.map(({ type, navn }) => (
                    <option key={type} value={type}>
                      {navn}
                    </option>
                  ))}
                </Select>
                {beskrivelse &&
                  beskrivelse.map((tekst, i) => <BodyShort key={i}>{tekst}</BodyShort>)}
                <PrismodellForm
                  prismodell={type}
                  avtaleStartDato={avtaleStartDato}
                  field={`prismodeller.${index}`}
                />
                <HStack>
                  <Button
                    variant="secondary-neutral"
                    size="small"
                    type="button"
                    icon={<TrashIcon aria-hidden />}
                    onClick={() => remove(index)}
                    aria-label="Fjern prismodell"
                  >
                    Fjern
                  </Button>
                </HStack>
              </VStack>
            </HStack>
          </Box>
        );
      })}
      <HStack>
        <Button
          icon={<PlusIcon aria-hidden />}
          type="button"
          variant="tertiary"
          size="small"
          onClick={() =>
            append({
              type: "" as PrismodellType,
              satser: [],
              prisbetingelser: null,
            })
          }
        >
          Legg til prismodell
        </Button>
      </HStack>
    </VStack>
  );
}

function erPrismodellMedAvtalteSatser(prismodell: PrismodellType) {
  switch (prismodell) {
    case PrismodellType.ANNEN_AVTALT_PRIS:
    case PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
      return false;
    case PrismodellType.AVTALT_PRIS_PER_MANEDSVERK:
    case PrismodellType.AVTALT_PRIS_PER_UKESVERK:
    case PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK:
    case PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
      return true;
  }
}
