import { zodResolver } from "@hookform/resolvers/zod";
import { FileTextIcon, LinkIcon, PaperplaneIcon, PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import { Alert, Box, Button, Heading, HStack, Tabs, VStack } from "@navikt/ds-react";
import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { PortableTextFormEditor } from "@/components/portableText/PortableTextEditor";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { RedaksjoneltInnholdTabTittel } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdTabTittel";
import { DescriptionRichtextContainer } from "@/components/redaksjoneltInnhold/DescriptionRichtextContainer";
import { usePatchTiltakstypeRedaksjoneltInnhold } from "@/api/tiltakstyper/usePatchTiltakstypeRedaksjoneltInnhold";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { ControlledMultiSelect } from "@/components/skjema/ControlledMultiSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { FormButtons } from "@/components/skjema/FormButtons";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import {
  TiltakstypeRedaksjoneltInnholdFormValues,
  TiltakstypeRedaksjoneltInnholdSchema,
} from "@/schemas/tiltakstypeRedaksjoneltInnhold";
import { FormProvider, useFieldArray, useForm, useFormContext } from "react-hook-form";

interface Props {
  tiltakstype: TiltakstypeDto;
  onSuccess: () => void;
  onCancel?: () => void;
}

export function TiltakstypeRedaksjoneltInnholdForm({ tiltakstype, onSuccess, onCancel }: Props) {
  const mutation = usePatchTiltakstypeRedaksjoneltInnhold(tiltakstype.id);
  const { data: alleTiltakstyper } = useTiltakstyper();

  const methods = useForm<TiltakstypeRedaksjoneltInnholdFormValues>({
    resolver: zodResolver(TiltakstypeRedaksjoneltInnholdSchema) as any,
    defaultValues: {
      beskrivelse: tiltakstype.beskrivelse || null,
      faneinnhold: tiltakstype.faneinnhold,
      regelverklenker: tiltakstype.regelverklenker,
      kanKombineresMed: alleTiltakstyper
        .filter((t) => tiltakstype.kanKombineresMed.includes(t.navn))
        .map((t) => t.id),
    },
  });

  const { handleSubmit } = methods;

  async function onSubmit(values: TiltakstypeRedaksjoneltInnholdFormValues) {
    await mutation.mutateAsync({
      beskrivelse: values.beskrivelse ?? null,
      faneinnhold: {
        forHvemInfoboks: values.faneinnhold?.forHvemInfoboks || null,
        forHvem: values.faneinnhold?.forHvem || null,
        detaljerOgInnholdInfoboks: values.faneinnhold?.detaljerOgInnholdInfoboks || null,
        detaljerOgInnhold: values.faneinnhold?.detaljerOgInnhold || null,
        pameldingOgVarighetInfoboks: values.faneinnhold?.pameldingOgVarighetInfoboks || null,
        pameldingOgVarighet: values.faneinnhold?.pameldingOgVarighet || null,
        kontaktinfo: values.faneinnhold?.kontaktinfo || null,
        kontaktinfoInfoboks: values.faneinnhold?.kontaktinfoInfoboks || null,
        lenker: values.faneinnhold?.lenker || null,
        delMedBruker: values.faneinnhold?.delMedBruker || null,
        oppskrift: [],
      },
      regelverklenker: values.regelverklenker.map((lenke) => ({
        regelverkUrl: lenke.regelverkUrl,
        regelverkLenkeNavn: lenke.regelverkLenkeNavn || null,
        beskrivelse: lenke.beskrivelse || null,
      })),
      kanKombineresMed: values.kanKombineresMed,
    });
    onSuccess();
  }

  const andreKombinasjonOptions = alleTiltakstyper
    .filter((t) => t.id !== tiltakstype.id)
    .map((t) => ({ value: t.id, label: t.navn }));

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <FormButtons
          submitLabel="Lagre redaksjonelt innhold"
          onCancel={onCancel}
          isPending={mutation.isPending}
        />
        <Separator />
        <TwoColumnGrid separator>
          <RedaksjoneltInnholdContainer>
            <Heading size="medium" level="3">
              Generell informasjon
            </Heading>

            <Alert size="small" variant="info">
              Ikke del personopplysninger i fritekstfeltene
            </Alert>

            <FormTextarea<TiltakstypeRedaksjoneltInnholdFormValues>
              name="beskrivelse"
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
                  <FormTextarea<TiltakstypeRedaksjoneltInnholdFormValues>
                    name="faneinnhold.forHvemInfoboks"
                    label='Fremhevet informasjon i blå infoboks under fanen "For hvem"'
                    rows={3}
                  />
                  <PortableTextFormEditor name="faneinnhold.forHvem" label="For hvem" />
                </DescriptionRichtextContainer>
              </Tabs.Panel>

              <Tabs.Panel value="detaljer_og_innhold">
                <DescriptionRichtextContainer>
                  <FormTextarea<TiltakstypeRedaksjoneltInnholdFormValues>
                    name="faneinnhold.detaljerOgInnholdInfoboks"
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
                  <FormTextarea<TiltakstypeRedaksjoneltInnholdFormValues>
                    name="faneinnhold.pameldingOgVarighetInfoboks"
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
                  <FormTextarea<TiltakstypeRedaksjoneltInnholdFormValues>
                    name="faneinnhold.kontaktinfoInfoboks"
                    label='Fremhevet informasjon i blå infoboks under fanen "Kontaktinfo"'
                    rows={3}
                  />
                  <PortableTextFormEditor name="faneinnhold.kontaktinfo" label="Kontaktinfo" />
                </DescriptionRichtextContainer>
              </Tabs.Panel>

              <Tabs.Panel value="lenker">
                <FaneinnholdLenker />
              </Tabs.Panel>

              <Tabs.Panel value="del_med_bruker">
                <DescriptionRichtextContainer>
                  <FormTextarea<TiltakstypeRedaksjoneltInnholdFormValues>
                    name="faneinnhold.delMedBruker"
                    label="Informasjon som kan deles med bruker"
                    description="Informasjon om tiltaket som veileder kan dele med bruker."
                    rows={5}
                  />
                </DescriptionRichtextContainer>
              </Tabs.Panel>
            </Tabs>
          </RedaksjoneltInnholdContainer>

          <VStack gap="space-16" paddingBlock="space-4">
            <Heading size="medium" level="3">
              Kan kombineres med
            </Heading>
            <ControlledMultiSelect
              name="kanKombineresMed"
              label="Tiltakstyper som kan kombineres med denne"
              placeholder="Søk etter tiltakstyper..."
              size="small"
              options={andreKombinasjonOptions}
            />

            <Heading size="medium" level="3">
              Regelverk
            </Heading>
            <RegelverklenkerSkjema />
          </VStack>
        </TwoColumnGrid>
        <Separator />
        <FormButtons
          submitLabel="Lagre redaksjonelt innhold"
          onCancel={onCancel}
          isPending={mutation.isPending}
        />
      </form>
    </FormProvider>
  );
}

function RegelverklenkerSkjema() {
  const { control } = useFormContext<TiltakstypeRedaksjoneltInnholdFormValues>();
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
            <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
              name={`regelverklenker.${index}.regelverkUrl`}
              label="URL"
            />
            <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
              name={`regelverklenker.${index}.regelverkLenkeNavn`}
              label="Lenketekst"
            />
            <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
              name={`regelverklenker.${index}.beskrivelse`}
              label="Beskrivelse (intern)"
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
  const { control } = useFormContext<TiltakstypeRedaksjoneltInnholdFormValues>();
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
            <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
              name={`faneinnhold.lenker.${index}.lenkenavn`}
              label="Lenkenavn"
            />
            <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
              name={`faneinnhold.lenker.${index}.lenke`}
              label="Lenke"
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
