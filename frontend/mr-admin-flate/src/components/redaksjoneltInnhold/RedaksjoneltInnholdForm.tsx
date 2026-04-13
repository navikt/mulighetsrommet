import { Alert, BodyLong, Box, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { DescriptionRichtextContainer } from "@/components/redaksjoneltInnhold/DescriptionRichtextContainer";
import { RedaksjoneltInnholdTabs } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdTabs";
import { PortableText } from "@mr/frontend-common";
import { PortableTextFormEditor } from "../portableText/PortableTextEditor";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { FormTextField } from "@/components/skjema/FormTextField";
import { useFieldArray, useFormContext } from "react-hook-form";
import { FormCheckbox } from "@/components/skjema/FormCheckbox";

interface Props {
  path: string;
  description: string;
  tiltakstype?: TiltakstypeDto;
}

export function RedaksjoneltInnholdForm({ path, description, tiltakstype }: Props) {
  const fp = (suffix: string) => [path, suffix].filter(Boolean).join(".");

  return (
    <RedaksjoneltInnholdContainer>
      <Heading size="medium" spacing level="3">
        Redaksjonelt innhold
      </Heading>
      <Alert size="small" variant="info">
        Ikke del personopplysninger i fritekstfeltene
      </Alert>
      <FormTextarea name={fp("beskrivelse")} description={description} label="Beskrivelse" />
      {tiltakstype?.beskrivelse && (
        <>
          <Heading size="medium">Generell informasjon</Heading>
          <BodyLong style={{ whiteSpace: "pre-wrap" }}>{tiltakstype.beskrivelse}</BodyLong>
        </>
      )}
      <Heading size="medium">Faneinnhold</Heading>
      <RedaksjoneltInnholdTabs
        forHvem={<ForHvem tiltakstype={tiltakstype} path={path} />}
        detaljerOgInnhold={<DetaljerOgInnhold tiltakstype={tiltakstype} path={path} />}
        pameldingOgVarighet={<PameldingOgVarighet tiltakstype={tiltakstype} path={path} />}
        kontaktinfo={<Kontaktinfo path={path} />}
        lenker={<RedaksjoneltInnholdLenkerForm path={path} />}
        delMedBruker={<DelMedBruker tiltakstype={tiltakstype} path={path} />}
      />
    </RedaksjoneltInnholdContainer>
  );
}

interface TabPanelProps {
  path: string;
  tiltakstype?: TiltakstypeDto;
}

function ForHvem({ tiltakstype, path }: TabPanelProps) {
  const fp = (suffix: string) => [path, suffix].filter(Boolean).join(".");

  return (
    <VStack className="mt-4">
      {tiltakstype?.faneinnhold?.forHvemInfoboks && (
        <Alert style={{ whiteSpace: "pre-wrap" }} variant="info">
          {tiltakstype.faneinnhold.forHvemInfoboks}
        </Alert>
      )}
      {tiltakstype?.faneinnhold?.forHvem && (
        <PortableText value={tiltakstype.faneinnhold.forHvem} />
      )}
      <Separator />
      <DescriptionRichtextContainer>
        <FormTextarea
          name={fp("faneinnhold.forHvemInfoboks")}
          label="Fremhevet informasjon til veileder som legger seg i blå infoboks i fanen «For hvem»"
          description="Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne."
        />
        <PortableTextFormEditor
          name={fp("faneinnhold.forHvem")}
          label="For hvem"
          description="Beskrivelse av hvem tiltakstypen passer for. Husk å bruke et kort og konsist språk."
        />
      </DescriptionRichtextContainer>
    </VStack>
  );
}

function DetaljerOgInnhold({ tiltakstype, path }: TabPanelProps) {
  const fp = (suffix: string) => [path, suffix].filter(Boolean).join(".");

  return (
    <VStack className="mt-4">
      {tiltakstype?.faneinnhold?.detaljerOgInnholdInfoboks && (
        <Alert variant="info">{tiltakstype.faneinnhold.detaljerOgInnholdInfoboks}</Alert>
      )}
      {tiltakstype?.faneinnhold?.detaljerOgInnhold && (
        <PortableText value={tiltakstype.faneinnhold.detaljerOgInnhold} />
      )}
      <Separator />

      <DescriptionRichtextContainer>
        <FormTextarea
          name={fp("faneinnhold.detaljerOgInnholdInfoboks")}
          label="Fremhevet informasjon til veileder som legger seg i blå infoboks i fanen «Detaljer og innhold»"
          description="Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne."
        />
        <PortableTextFormEditor
          name={fp("faneinnhold.detaljerOgInnhold")}
          label="Detaljer og innhold"
          description="Beskrivelse av detaljer og innhold for tiltakstypen. Husk å bruke et kort og konsist språk."
        />
      </DescriptionRichtextContainer>
    </VStack>
  );
}

function PameldingOgVarighet({ tiltakstype, path }: TabPanelProps) {
  const fp = (suffix: string) => [path, suffix].filter(Boolean).join(".");

  return (
    <VStack className="mt-4">
      {tiltakstype?.faneinnhold?.pameldingOgVarighetInfoboks && (
        <Alert variant="info">{tiltakstype.faneinnhold.pameldingOgVarighetInfoboks}</Alert>
      )}
      {tiltakstype?.faneinnhold?.pameldingOgVarighet && (
        <PortableText value={tiltakstype.faneinnhold.pameldingOgVarighet} />
      )}
      <Separator />

      <DescriptionRichtextContainer>
        <FormTextarea
          name={fp("faneinnhold.pameldingOgVarighetInfoboks")}
          label="Fremhevet informasjon til veileder som legger seg i blå infoboks i fanen «Påmelding og varighet»"
          description="Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne."
        />
        <PortableTextFormEditor
          name={fp("faneinnhold.pameldingOgVarighet")}
          label="Påmelding og varighet"
          description="Beskrivelse av rutiner rundt påmelding og varighet i tiltaket. Husk å bruke et kort og konsist språk."
        />
      </DescriptionRichtextContainer>
    </VStack>
  );
}

function Kontaktinfo({ path }: { path: string }) {
  const fp = (suffix: string) => [path, suffix].filter(Boolean).join(".");

  return (
    <VStack className="mt-4">
      <VStack gap="space-20">
        <FormTextarea
          name={fp("faneinnhold.kontaktinfoInfoboks")}
          label="Fremhevet informasjon til veileder som legger seg i blå infoboks i fanen «Kontaktinfo»"
          description="Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne."
        />
        <PortableTextFormEditor
          name={fp("faneinnhold.kontaktinfo")}
          label="Kontaktinfo"
          description="Ekstra tekst om kontaktinfo."
        />
      </VStack>
    </VStack>
  );
}

function DelMedBruker({ path }: TabPanelProps) {
  const fp = (suffix: string) => [path, suffix].filter(Boolean).join(".");

  return (
    <VStack className="mt-4">
      <FormTextarea
        name={fp("faneinnhold.delMedBruker")}
        label="Del med bruker"
        description='Bruk denne tekstboksen for å redigere teksten som sendes til bruker når man deler et tiltak. Det blir automatisk lagt til en "Hei" og en "Hilsen".'
      />
    </VStack>
  );
}

function RedaksjoneltInnholdLenkerForm({ path }: TabPanelProps) {
  return (
    <Box padding={"space-8"}>
      <VStack>
        <Heading style={{ marginBottom: "1rem" }} level="4" size="small">
          Legg til lenker
        </Heading>
        <HStack gap="space-80">
          <LenkerFields path={path} />
        </HStack>
      </VStack>
    </Box>
  );
}

function LenkerFields({ path }: TabPanelProps) {
  const fp = (suffix: string) => [path, suffix].filter(Boolean).join(".");
  const { control } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control,
    name: fp("faneinnhold.lenker"),
  });

  return (
    <VStack gap="space-20">
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
      <VStack gap="space-20" className="max-h-[50rem] overflow-auto p-4">
        {fields.map((lenke, index) => {
          return (
            <VStack gap="space-8" key={lenke.id}>
              <FormTextField name={fp(`faneinnhold.lenker.${index}.lenkenavn`)} label="Lenkenavn" />
              <FormTextField name={fp(`faneinnhold.lenker.${index}.lenke`)} label="Lenke" />
              <HStack gap="space-8">
                <FormCheckbox name={fp(`faneinnhold.lenker.${index}.apneINyFane`)}>
                  Åpne i ny fane
                </FormCheckbox>
                <FormCheckbox name={fp(`faneinnhold.lenker.${index}.visKunForVeileder`)}>
                  Vis kun i Modia
                </FormCheckbox>
              </HStack>
              <HStack justify="end">
                <Button
                  data-color="danger"
                  size="small"
                  variant="primary"
                  type="button"
                  onClick={() => remove(index)}
                >
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
