import {
  BodyShort,
  Box,
  Button,
  Select,
  VStack,
  HStack,
  Textarea,
  InlineMessage,
} from "@navikt/ds-react";
import { useFormContext, useFieldArray } from "react-hook-form";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { PrismodellValues } from "@/schemas/avtale";
import { usePrismodeller } from "@/api/avtaler/usePrismodeller";
import { AvtalteSatserForm } from "./AvtalteSatserForm";
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
    register,
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
                  label={avtaletekster.prismodell.label}
                  size="small"
                  error={errors.prismodeller?.[index]?.type?.message}
                  value={type}
                  onChange={(e) => {
                    setValue(`prismodeller.${index}.type`, e.target.value as PrismodellType);
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
                {type !== PrismodellType.ANNEN_AVTALT_PRIS && (
                  <AvtalteSatserForm
                    avtaleStartDato={avtaleStartDato}
                    field={`prismodeller.${index}`}
                  />
                )}
                <Textarea
                  size="small"
                  error={errors.prismodeller?.[index]?.prisbetingelser?.message}
                  label={avtaletekster.prisOgBetalingLabel}
                  {...register(`prismodeller.${index}.prisbetingelser` as const)}
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
                    Fjern prismodell
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
      {errors.prismodeller?.message && (
        <InlineMessage status="error">{errors.prismodeller.message}</InlineMessage>
      )}
    </VStack>
  );
}
