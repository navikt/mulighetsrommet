import { GjennomforingDto } from "@mr/api-client-v2";
import { TilsagnBeregningType } from "@tiltaksadministrasjon/api-client";
import { TilsagnRequest } from "@tiltaksadministrasjon/api-client";
import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { useFieldArray, useFormContext } from "react-hook-form";
import {
  Alert,
  Button,
  HStack,
  Label,
  Spacer,
  Textarea,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Metadata } from "@/components/detaljside/Metadata";
import { tilsagnTekster } from "../TilsagnTekster";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: TilsagnRequest;
  regioner: string[];
}

export function TilsagnFormFri(props: Props) {
  return <TilsagnForm {...props} beregningInput={<BeregningInputSkjema />} />;
}

function BeregningInputSkjema() {
  const {
    register,
    watch,
    formState: { errors },
    control,
  } = useFormContext<TilsagnRequest>();
  const { fields, append, remove } = useFieldArray({ control, name: "beregning.linjer" });

  const prisbetingelser = watch("beregning.prisbetingelser");

  return (
    <VStack gap="4">
      <Metadata
        header={tilsagnTekster.prismodell.label}
        value={tilsagnTekster.prismodell.sats.label(TilsagnBeregningType.FRI)}
      />
      <Textarea
        size="small"
        label={avtaletekster.prisOgBetalingLabel}
        value={prisbetingelser ?? ""}
        readOnly
      />
      <Label size="small">Avtalte priser</Label>
      {fields.map((item, index) => (
        <HStack
          gap="4"
          align="start"
          key={item.id}
          padding="4"
          className="border-border-subtle border-1 rounded-lg"
        >
          <Textarea
            size="small"
            label="Beskrivelse"
            className="flex-3"
            error={errors.beregning?.linjer?.[index]?.beskrivelse?.message}
            {...register(`beregning.linjer.${index}.beskrivelse`)}
            defaultValue={item.beskrivelse ?? ""}
          />
          <TextField
            size="small"
            type="number"
            label="Beløp"
            className="flex-2"
            error={errors.beregning?.linjer?.[index]?.belop?.message}
            {...register(`beregning.linjer.${index}.belop`, {
              setValueAs: (v) => (v === "" ? null : Number(v)),
            })}
            defaultValue={item.belop ?? 0}
          />
          <TextField
            size="small"
            type="number"
            label="Antall"
            className="flex-1"
            error={errors.beregning?.linjer?.[index]?.antall?.message}
            {...register(`beregning.linjer.${index}.antall`, {
              setValueAs: (v) => (v === "" ? null : Number(v)),
            })}
            defaultValue={item.antall ?? 0}
          />
          <input
            type="hidden"
            {...register(`beregning.linjer.${index}.id`)}
            defaultValue={item.id}
          />
          <Spacer />
          <Button
            size="small"
            variant="secondary-neutral"
            icon={<TrashIcon aria-hidden />}
            className="max-h-min self-center"
            onClick={() => remove(index)}
          >
            Fjern
          </Button>
        </HStack>
      ))}
      {errors.beregning?.linjer?.message && (
        <Alert size="small" variant="error">
          {errors.beregning.linjer.message}
        </Alert>
      )}
      <Button
        size="small"
        variant="tertiary"
        icon={<PlusIcon aria-hidden />}
        className="self-end"
        onClick={(event) => {
          event.preventDefault();
          event.stopPropagation();
          append({ id: window.crypto.randomUUID(), beskrivelse: "", belop: 0, antall: 1 });
        }}
      >
        Legg til ny pris
      </Button>
    </VStack>
  );
}
