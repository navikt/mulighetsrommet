import { Box, Button, Heading, HStack, Switch, TextField, VStack } from "@navikt/ds-react";
import { useFieldArray, useFormContext } from "react-hook-form";
import z from "zod";
import { FaneinnholdLenkerSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";

type Lenke = {
  veilederinformasjon: {
    faneinnhold: {
      lenker: z.infer<typeof FaneinnholdLenkerSchema>;
    };
  };
};

export function Lenker() {
  return (
    <Box padding={"2"}>
      <VStack>
        <Heading style={{ marginBottom: "1rem" }} level="4" size="small">
          Legg til lenker
        </Heading>
        <HStack gap="20">
          <LenkerSkjema />
        </HStack>
      </VStack>
    </Box>
  );
}

function LenkerSkjema() {
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<Lenke>();
  const { fields, append, remove } = useFieldArray({
    control,
    name: "veilederinformasjon.faneinnhold.lenker",
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
      <VStack gap="5" className="max-h-[50rem] overflow-auto p-4">
        {fields.map((lenke, index) => {
          return (
            <VStack gap="2" key={lenke.id}>
              <TextField
                size="small"
                label="Lenkenavn"
                {...register(`veilederinformasjon.faneinnhold.lenker.${index}.lenkenavn`)}
                error={errors.veilederinformasjon?.faneinnhold?.lenker?.[index]?.lenkenavn?.message}
              />
              <TextField
                size="small"
                label="Lenke"
                {...register(`veilederinformasjon.faneinnhold.lenker.${index}.lenke`)}
                error={errors.veilederinformasjon?.faneinnhold?.lenker?.[index]?.lenke?.message}
              />
              <HStack gap="2">
                <Switch
                  {...register(`veilederinformasjon.faneinnhold.lenker.${index}.apneINyFane`)}
                >
                  Åpne i ny fane
                </Switch>
                <Switch
                  {...register(`veilederinformasjon.faneinnhold.lenker.${index}.visKunForVeileder`)}
                >
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
