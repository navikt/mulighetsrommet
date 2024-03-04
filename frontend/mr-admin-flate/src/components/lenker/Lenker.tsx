import { Box, Button, HStack, Heading, Switch, TextField, VStack } from "@navikt/ds-react";
import { useFieldArray, useFormContext } from "react-hook-form";
import z from "zod";
import styles from "./Lenker.module.scss";
import { FaneinnholdSchema } from "../redaksjonelt-innhold/FaneinnholdSchema";

const LenkeSchema = z.object({
  faneinnhold: FaneinnholdSchema.pick({ lenker: true }),
});
type Lenke = z.infer<typeof LenkeSchema>;

export function Lenker() {
  return (
    <Box padding={"2"}>
      <VStack>
        <Heading style={{ marginBottom: "1rem" }} level="4" size="small">
          Legg til lenker
        </Heading>
        <HStack gap="20">
          <Lenkeskjema />
        </HStack>
      </VStack>
    </Box>
  );
}

function Lenkeskjema() {
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<Lenke>();
  const { fields, append, remove } = useFieldArray({
    control,
    name: "faneinnhold.lenker",
  });

  return (
    <VStack gap="5">
      <Button
        type="button"
        size="small"
        variant="primary"
        onClick={() =>
          append({ lenke: "", lenkenavn: "", visKunForVeileder: false, apneINyFane: false })
        }
      >
        Registrer ny lenke
      </Button>
      <VStack gap="5" className={styles.lenkeliste}>
        {fields?.map((lenke, index) => {
          return (
            <VStack gap="2" key={lenke.id}>
              <TextField
                label="Lenkenavn"
                {...register(`faneinnhold.lenker.${index}.lenkenavn`)}
                error={errors?.faneinnhold?.lenker?.[index]?.lenkenavn?.message}
              />
              <TextField
                label="Lenke"
                {...register(`faneinnhold.lenker.${index}.lenke`)}
                error={errors?.faneinnhold?.lenker?.[index]?.lenke?.message}
              />
              <HStack gap="2">
                <Switch {...register(`faneinnhold.lenker.${index}.apneINyFane`)}>
                  Åpne i ny fane
                </Switch>
                <Switch {...register(`faneinnhold.lenker.${index}.visKunForVeileder`)}>
                  Vis kun i Modia
                </Switch>
              </HStack>
              <HStack justify="end">
                <Button size="small" variant="danger" type="button" onClick={() => remove(index)}>
                  Slett lenke
                </Button>
              </HStack>
            </VStack>
          );
        })}
      </VStack>
    </VStack>
  );
}
