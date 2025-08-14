import { GjennomforingDto, TilsagnBeregningFri, TilsagnBeregningType } from "@mr/api-client-v2";
import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { DeepPartial, useFieldArray, useFormContext } from "react-hook-form";
import {
  Alert,
  Button,
  Heading,
  HStack,
  Textarea,
  TextField,
  Tooltip,
  VStack,
} from "@navikt/ds-react";
import { TilsagnBeregningPreview } from "@/components/tilsagn/form/TilsagnBeregningPreview";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { TrashIcon } from "@navikt/aksel-icons";
import { InferredTilsagn } from "./TilsagnSchema";

type FriTilsagn = InferredTilsagn & { beregning: TilsagnBeregningFri };

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<FriTilsagn>;
  regioner: string[];
}

export function TilsagnFormFri(props: Props) {
  return (
    <TilsagnForm
      {...props}
      beregningInput={<BeregningInputSkjema />}
      beregningOutput={<BeregningOutputPreview />}
    />
  );
}

function BeregningInputSkjema() {
  const {
    register,
    watch,
    formState: { errors },
  } = useFormContext<FriTilsagn>();

  return (
    <VStack gap="4">
      <Heading size="small">Prismodell - Annen avtalt pris</Heading>
      {watch("beregning.prisbetingelser") && (
        <div className="pb-3">
          <Textarea
            size="small"
            label={avtaletekster.prisOgBetalingLabel}
            readOnly
            error={errors.beregning?.prisbetingelser?.message}
            {...register("beregning.prisbetingelser")}
          />
        </div>
      )}
      <BeregningInputLinjerSkjema />
    </VStack>
  );
}

function BeregningInputLinjerSkjema() {
  const {
    register,
    formState: { errors },
    setError,
    control,
  } = useFormContext<FriTilsagn>();
  const { fields, append, remove } = useFieldArray({ control, name: "beregning.linjer" });
  const linjer = fields.map((item, index) => (
    <HStack gap="4" key={item.id}>
      <div className="mt-7">
        <b>{index + 1}</b>
      </div>
      <Textarea
        className="flex-1"
        size="small"
        label="Beskrivelse"
        error={errors.beregning?.linjer?.[index]?.beskrivelse?.message}
        {...register(`beregning.linjer.${index}.beskrivelse`)}
        defaultValue={item.beskrivelse}
      />
      <div>
        <TextField
          size="small"
          type="number"
          label="Beløp"
          style={{ width: "180px" }}
          error={errors.beregning?.linjer?.[index]?.belop?.message}
          {...register(`beregning.linjer.${index}.belop`, { valueAsNumber: true })}
          defaultValue={item.belop}
        />
      </div>
      <div>
        <TextField
          size="small"
          type="number"
          label="Antall"
          style={{ width: "180px" }}
          error={errors.beregning?.linjer?.[index]?.antall?.message}
          {...register(`beregning.linjer.${index}.antall`, { valueAsNumber: true })}
          defaultValue={item.antall}
        />
      </div>
      <div>
        <TextField
          label="Linje-id"
          hideLabel
          hidden
          {...register(`beregning.linjer.${index}.id`)}
          defaultValue={item.id}
        />
        <Tooltip content={`Slett linje ${index + 1}`}>
          <Button
            className="mt-7"
            size="small"
            variant="danger"
            icon={<TrashIcon aria-hidden />}
            onClickCapture={(e) => {
              e.preventDefault();
              e.stopPropagation();
              remove(index);
            }}
          />
        </Tooltip>
      </div>
    </HStack>
  ));
  return (
    <VStack className="mt-4" gap="4">
      {linjer}
      {errors.beregning?.linjer?.message && (
        <Alert size="small" variant="error">
          {errors.beregning?.linjer?.message}
        </Alert>
      )}
      <div>
        <Button
          size="small"
          variant="secondary"
          onClickCapture={(e) => {
            e.preventDefault();
            e.stopPropagation();
            setError("beregning.linjer", {});
            append({ id: window.crypto.randomUUID(), beskrivelse: "", belop: 0, antall: 1 });
          }}
        >
          Legg til ny linje
        </Button>
      </div>
    </VStack>
  );
}

function BeregningOutputPreview() {
  const { watch } = useFormContext<FriTilsagn>();

  const values = watch();
  const linjer = values.beregning?.linjer || [];
  return (
    <>
      <TilsagnBeregningPreview
        input={{
          type: TilsagnBeregningType.FRI,
          linjer: linjer,
          prisbetingelser: values.beregning?.prisbetingelser,
        }}
      />
    </>
  );
}
