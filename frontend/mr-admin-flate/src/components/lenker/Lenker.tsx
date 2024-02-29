import { Box, Button, HStack, Heading, Switch, TextField, VStack } from "@navikt/ds-react";
import { FieldValues, useFieldArray, useFormContext } from "react-hook-form";
import { InferredAvtaleSchema } from "../avtaler/AvtaleSchema";
import styles from "./Lenker.module.scss";

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

//  TODO Gjøre komponenten generisk så den kan brukes i gjennomføringsskjema by default
// TODO Må kunne ta i mot lenker fra avtalenivå ned til tiltaksgjennomføring
interface Props {
  fields: Record<"id", string>[];
  append: () => void;
  remove: () => void;
}

function Lenkeskjema() {
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<InferredAvtaleSchema>(); // TODO Må ta denne inn som generisk verdi
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
