import { useFieldArray, useFormContext } from "react-hook-form";
import { memo } from "react";
import { Prismodell, Tiltakskode } from "@mr/api-client-v2";
import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Box, Button, HStack, Select, Spacer, Textarea, TextField, VStack } from "@navikt/ds-react";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { addDuration, formaterDato } from "@mr/frontend-common/utils/date";
import { useForhandsgodkjenteSatser } from "@/api/avtaler/useForhandsgodkjenteSatser";
import { AvtaleFormValues } from "@/schemas/avtale";

interface Props {
  tiltakskode: Tiltakskode;
  prismodell?: Prismodell;
  avtaleStartDato: Date;
}

const PrismodellForm = memo(({ tiltakskode, prismodell, avtaleStartDato }: Props) => {
  switch (prismodell) {
    case Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
      return <ForhandsgodkjenteSatser tiltakskode={tiltakskode} />;
    case Prismodell.AVTALT_PRIS_PER_MANEDSVERK:
    case Prismodell.AVTALT_PRIS_PER_UKESVERK:
    case Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
      return <AvtalteSatser fromDate={avtaleStartDato} />;
    case Prismodell.ANNEN_AVTALT_PRIS:
    case undefined:
      return <PrisbetingelserTextArea />;
  }
});

function ForhandsgodkjenteSatser({ tiltakskode }: { tiltakskode: Tiltakskode }) {
  const { data: satser = [] } = useForhandsgodkjenteSatser(tiltakskode);
  if (satser.length === 0) return null;
  return (
    <VStack gap="4">
      {satser.map((sats) => (
        <Box
          padding="4"
          borderColor="border-subtle"
          borderRadius="large"
          borderWidth="1"
          key={sats.gjelderFra}
        >
          <HStack key={sats.gjelderFra} gap="4">
            <TextField
              readOnly
              label={avtaletekster.prismodell.valuta.label}
              size="small"
              value={sats.valuta}
            />
            <TextField
              readOnly
              label={avtaletekster.prismodell.sats.label}
              size="small"
              value={sats.pris}
            />
            <TextField
              readOnly
              label={avtaletekster.prismodell.periodeStart.label}
              size="small"
              value={formaterDato(sats.gjelderFra)}
            />
          </HStack>
        </Box>
      ))}
    </VStack>
  );
}

function AvtalteSatser({ fromDate }: { fromDate: Date }) {
  const {
    control,
    register,
    setValue,
    getValues,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();

  const { fields, append, remove } = useFieldArray({
    name: "prismodell.satser",
    control,
  });

  // Flere aktive avtaler har start i 2001, 2010 osv, 30 år holder enn så lenge men
  // burde ha en bedre løsning her. F. eks ikke bruk datepicker, men tekstfelt
  const toDate = addDuration(fromDate, { years: 30 });

  return (
    <VStack gap="4">
      {fields.map((field, index) => (
        <HStack
          key={field.gjelderFra}
          padding="4"
          gap="4"
          wrap={false}
          align="center"
          className="border-border-subtle border-1 rounded-lg"
        >
          <HStack key={field.gjelderFra} gap="4" align="start">
            <Select readOnly label="Valuta" size="small">
              <option value={undefined}>{field.valuta}</option>
            </Select>
            <TextField
              label={avtaletekster.prismodell.pris.label}
              size="small"
              type="number"
              error={errors.prismodell?.satser?.[index]?.pris?.message}
              {...register(`prismodell.satser.${index}.pris`, {
                valueAsNumber: true,
              })}
            />
            <ControlledDateInput
              label={avtaletekster.prismodell.periodeStart.label}
              fromDate={fromDate}
              toDate={toDate}
              onChange={(val) => setValue(`prismodell.satser.${index}.gjelderFra`, val)}
              error={errors.prismodell?.satser?.[index]?.gjelderFra?.message}
              defaultSelected={getValues(`prismodell.satser.${index}.gjelderFra`)}
            />
          </HStack>
          <Spacer />
          <Button
            variant="secondary-neutral"
            size="small"
            type="button"
            icon={<TrashIcon aria-hidden />}
            onClick={() => remove(index)}
            className="max-h-min"
          >
            Fjern
          </Button>
        </HStack>
      ))}
      <Button
        className="self-end"
        variant="tertiary"
        size="small"
        type="button"
        icon={<PlusIcon aria-hidden />}
        onClick={() => append({ gjelderFra: null, pris: 0, valuta: "NOK" })}
      >
        Legg til ny prisperiode
      </Button>
      <PrisbetingelserTextArea />
    </VStack>
  );
}

function PrisbetingelserTextArea() {
  const {
    register,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();

  return (
    <Textarea
      size="small"
      error={errors.prismodell?.prisbetingelser?.message}
      label={avtaletekster.prisOgBetalingLabel}
      {...register("prismodell.prisbetingelser")}
    />
  );
}

export default PrismodellForm;
