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
import { PrismodellType, Tiltakskode, Valuta } from "@tiltaksadministrasjon/api-client";
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
  const valutaOptions = [Valuta.NOK, Valuta.SEK];

  const prismodellerMedSatser = [
    PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
    PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
    PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
    PrismodellType.AVTALT_PRIS_PER_UKESVERK,
  ];

  const { fields, append, remove } = useFieldArray({
    name: "prismodeller",
    control,
  });

  const onPrismodelltypeChange = (index: number, type: PrismodellType) => {
    setValue(`prismodeller.${index}.type`, type);
    setValue(
      `prismodeller.${index}.satser`,
      [PrismodellType.ANNEN_AVTALT_PRIS_PER_DELTAKER, PrismodellType.ANNEN_AVTALT_PRIS].includes(
        type,
      )
        ? []
        : [{ gjelderTil: null, gjelderFra: "", pris: 0 }],
    );
  };

  return (
    <VStack gap="space-16">
      {fields.map((field, index) => {
        const type = watch(`prismodeller.${index}.type`);
        const selectedValuta = watch(`prismodeller.${index}.valuta`);
        const beskrivelse = prismodellTyper.find((p) => p.type === type)?.beskrivelse;
        return (
          <Box
            key={field.id}
            borderWidth="1"
            borderColor="neutral-subtle"
            borderRadius="8"
            padding="space-16"
            background="neutral-soft"
          >
            <HStack justify="space-between" align="start">
              <VStack gap="space-16" style={{ flex: 1 }}>
                <HStack gap="space-8">
                  <Select
                    className="flex-1"
                    label={avtaletekster.prismodell.label}
                    size="small"
                    error={errors.prismodeller?.[index]?.type?.message}
                    value={type}
                    onChange={(e) =>
                      onPrismodelltypeChange(index, e.target.value as PrismodellType)
                    }
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
                  <Select
                    label="Valuta"
                    size="small"
                    value={selectedValuta}
                    onChange={(e) => {
                      setValue(`prismodeller.${index}.valuta`, e.target.value as Valuta);
                    }}
                  >
                    {valutaOptions.map((valuta) => (
                      <option key={valuta} value={valuta}>
                        {valuta}
                      </option>
                    ))}
                  </Select>
                </HStack>

                {beskrivelse &&
                  beskrivelse.map((tekst, i) => <BodyShort key={i}>{tekst}</BodyShort>)}
                {prismodellerMedSatser.includes(type) && (
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
                    data-color="neutral"
                    variant="secondary"
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
              valuta: Valuta.NOK,
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
