import { zodResolver } from "@hookform/resolvers/zod";
import { FileTextIcon, LinkIcon, PaperplaneIcon, PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import {
  Alert,
  Box,
  Button,
  Heading,
  HStack,
  Tabs,
  Textarea,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { PortableTextFormEditor } from "@/components/portableText/PortableTextEditor";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { RedaksjoneltInnholdTabTittel } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdTabTittel";
import { DescriptionRichtextContainer } from "@/components/redaksjoneltInnhold/DescriptionRichtextContainer";
import { usePatchTiltakstypeRedaksjoneltInnhold } from "@/api/tiltakstyper/usePatchTiltakstypeRedaksjoneltInnhold";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { ControlledMultiSelect } from "@/components/skjema/ControlledMultiSelect";
import {
  TiltakstypeRedaksjoneltInnholdFormValues,
  TiltakstypeRedaksjoneltInnholdSchema,
} from "@/schemas/tiltakstypeRedaksjoneltInnhold";
import { FormProvider, useFieldArray, useForm, useFormContext } from "react-hook-form";

interface Props {
  tiltakstype: TiltakstypeDto;
  onSuccess: () => void;
}

export function TiltakstypeRedaksjoneltInnholdForm({ tiltakstype, onSuccess }: Props) {
  const mutation = usePatchTiltakstypeRedaksjoneltInnhold(tiltakstype.id);
  const { data: alleTiltakstyper } = useTiltakstyper();

  const methods = useForm<TiltakstypeRedaksjoneltInnholdFormValues>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(TiltakstypeRedaksjoneltInnholdSchema) as any,
    defaultValues: {
      beskrivelse: tiltakstype.beskrivelse ?? null,
      faneinnhold: tiltakstype.faneinnhold ?? null,
      regelverklenker: tiltakstype.regelverklenker ?? [],
      kanKombineresMed:
        alleTiltakstyper
          ?.filter((t) => tiltakstype.kanKombineresMed.includes(t.navn))
          .map((t) => t.id) ?? [],
    },
  });

  const { handleSubmit } = methods;

  async function onSubmit(values: TiltakstypeRedaksjoneltInnholdFormValues) {
    await mutation.mutateAsync({
      beskrivelse: values.beskrivelse ?? null,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      faneinnhold: (values.faneinnhold as any) ?? null,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      regelverklenker: (values.regelverklenker as any) ?? [],
      kanKombineresMed: values.kanKombineresMed ?? [],
    });
    onSuccess();
  }

  const andreKombinasjonOptions =
    alleTiltakstyper
      ?.filter((t) => t.id !== tiltakstype.id)
      .map((t) => ({ value: t.id, label: t.navn })) ?? [];

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <VStack gap="space-16">
          <Alert size="small" variant="info">
            Ikke del personopplysninger i fritekstfeltene
          </Alert>

          <RedaksjoneltInnholdContainer>
            <Heading size="medium" level="3">
              Generell informasjon
            </Heading>
            <Textarea
              {...methods.register("beskrivelse")}
              label="Beskrivelse"
              description="Kort beskrivelse av formål med tiltakstypen."
              rows={5}
            />

            <Heading size="medium" level="3">
              Faneinnhold
            </Heading>
            <Tabs size="small" defaultValue="for_hvem">
              <Tabs.List>
                <Tabs.Tab
                  value="for_hvem"
                  label={
                    <RedaksjoneltInnholdTabTittel>
                      <FileTextIcon style={{ fontSize: "1.5rem" }} /> For hvem
                    </RedaksjoneltInnholdTabTittel>
                  }
                />
                <Tabs.Tab
                  value="detaljer_og_innhold"
                  label={
                    <RedaksjoneltInnholdTabTittel>
                      <FileTextIcon style={{ fontSize: "1.5rem" }} /> Detaljer og innhold
                    </RedaksjoneltInnholdTabTittel>
                  }
                />
                <Tabs.Tab
                  value="pamelding_og_varighet"
                  label={
                    <RedaksjoneltInnholdTabTittel>
                      <FileTextIcon style={{ fontSize: "1.5rem" }} /> Påmelding og varighet
                    </RedaksjoneltInnholdTabTittel>
                  }
                />
                <Tabs.Tab
                  value="kontaktinfo"
                  label={
                    <RedaksjoneltInnholdTabTittel>
                      <FileTextIcon style={{ fontSize: "1.5rem" }} /> Kontaktinfo
                    </RedaksjoneltInnholdTabTittel>
                  }
                />
                <Tabs.Tab
                  value="lenker"
                  label={
                    <RedaksjoneltInnholdTabTittel>
                      <LinkIcon style={{ fontSize: "1.5rem" }} /> Lenker
                    </RedaksjoneltInnholdTabTittel>
                  }
                />
                <Tabs.Tab
                  value="del_med_bruker"
                  label={
                    <RedaksjoneltInnholdTabTittel>
                      <PaperplaneIcon style={{ fontSize: "1.5rem" }} /> Del med bruker
                    </RedaksjoneltInnholdTabTittel>
                  }
                />
              </Tabs.List>

              <Tabs.Panel value="for_hvem">
                <DescriptionRichtextContainer>
                  <Textarea
                    {...methods.register("faneinnhold.forHvemInfoboks")}
                    label='Fremhevet informasjon i blå infoboks under fanen "For hvem"'
                    rows={3}
                  />
                  <PortableTextFormEditor
                    name="faneinnhold.forHvem"
                    label="For hvem"
                  />
                </DescriptionRichtextContainer>
              </Tabs.Panel>

              <Tabs.Panel value="detaljer_og_innhold">
                <DescriptionRichtextContainer>
                  <Textarea
                    {...methods.register("faneinnhold.detaljerOgInnholdInfoboks")}
                    label='Fremhevet informasjon i blå infoboks under fanen "Detaljer og innhold"'
                    rows={3}
                  />
                  <PortableTextFormEditor
                    name="faneinnhold.detaljerOgInnhold"
                    label="Detaljer og innhold"
                  />
                </DescriptionRichtextContainer>
              </Tabs.Panel>

              <Tabs.Panel value="pamelding_og_varighet">
                <DescriptionRichtextContainer>
                  <Textarea
                    {...methods.register("faneinnhold.pameldingOgVarighetInfoboks")}
                    label='Fremhevet informasjon i blå infoboks under fanen "Påmelding og varighet"'
                    rows={3}
                  />
                  <PortableTextFormEditor
                    name="faneinnhold.pameldingOgVarighet"
                    label="Påmelding og varighet"
                  />
                </DescriptionRichtextContainer>
              </Tabs.Panel>

              <Tabs.Panel value="kontaktinfo">
                <DescriptionRichtextContainer>
                  <Textarea
                    {...methods.register("faneinnhold.kontaktinfoInfoboks")}
                    label='Fremhevet informasjon i blå infoboks under fanen "Kontaktinfo"'
                    rows={3}
                  />
                  <PortableTextFormEditor
                    name="faneinnhold.kontaktinfo"
                    label="Kontaktinfo"
                  />
                </DescriptionRichtextContainer>
              </Tabs.Panel>

              <Tabs.Panel value="lenker">
                <FaneinnholdLenker />
              </Tabs.Panel>

              <Tabs.Panel value="del_med_bruker">
                <DescriptionRichtextContainer>
                  <Textarea
                    {...methods.register("faneinnhold.delMedBruker")}
                    label="Informasjon som kan deles med bruker"
                    description="Informasjon om tiltaket som veileder kan dele med bruker."
                    rows={5}
                  />
                </DescriptionRichtextContainer>
              </Tabs.Panel>
            </Tabs>

            <Heading size="medium" level="3">
              Regelverk
            </Heading>
            <RegelverklenkerSkjema />

            <Heading size="medium" level="3">
              Kan kombineres med
            </Heading>
            <ControlledMultiSelect
              name="kanKombineresMed"
              label="Tiltakstyper som kan kombineres med denne"
              placeholder="Søk etter tiltakstyper..."
              options={andreKombinasjonOptions}
            />
          </RedaksjoneltInnholdContainer>

          <HStack gap="space-8">
            <Button type="submit" loading={mutation.isPending}>
              Lagre
            </Button>
          </HStack>
        </VStack>
      </form>
    </FormProvider>
  );
}

function RegelverklenkerSkjema() {
  const { register, control } = useFormContext<TiltakstypeRedaksjoneltInnholdFormValues>();
  const { fields, append, remove } = useFieldArray({ control, name: "regelverklenker" });

  return (
    <VStack gap="space-12">
      <Button
        type="button"
        size="small"
        variant="secondary"
        icon={<PlusIcon />}
        onClick={() => append({ regelverkUrl: "", regelverkLenkeNavn: null, beskrivelse: null })}
      >
        Legg til regelverkslenke
      </Button>
      {fields.map((field, index) => (
        <Box key={field.id} padding="space-8" borderWidth="1" borderRadius="8">
          <VStack gap="space-8">
            <TextField
              size="small"
              label="URL"
              {...register(`regelverklenker.${index}.regelverkUrl`)}
            />
            <TextField
              size="small"
              label="Lenketekst"
              {...register(`regelverklenker.${index}.regelverkLenkeNavn`)}
            />
            <TextField
              size="small"
              label="Beskrivelse (intern)"
              {...register(`regelverklenker.${index}.beskrivelse`)}
            />
            <HStack justify="end">
              <Button
                type="button"
                size="small"
                variant="danger"
                icon={<XMarkIcon />}
                onClick={() => remove(index)}
              >
                Fjern
              </Button>
            </HStack>
          </VStack>
        </Box>
      ))}
    </VStack>
  );
}

function FaneinnholdLenker() {
  const { register, control } = useFormContext<TiltakstypeRedaksjoneltInnholdFormValues>();
  const { fields, append, remove } = useFieldArray({
    control,
    name: "faneinnhold.lenker",
  });

  return (
    <DescriptionRichtextContainer>
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
      <VStack gap="space-20">
        {fields.map((lenke, index) => (
          <VStack gap="space-8" key={lenke.id}>
            <TextField
              size="small"
              label="Lenkenavn"
              {...register(`faneinnhold.lenker.${index}.lenkenavn`)}
            />
            <TextField
              size="small"
              label="Lenke"
              {...register(`faneinnhold.lenker.${index}.lenke`)}
            />
            <HStack justify="end">
              <Button
                type="button"
                size="small"
                variant="danger"
                icon={<XMarkIcon />}
                onClick={() => remove(index)}
              >
                Fjern
              </Button>
            </HStack>
          </VStack>
        ))}
      </VStack>
    </DescriptionRichtextContainer>
  );
}
