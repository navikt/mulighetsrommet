import {
  Alert,
  BodyLong,
  Box,
  Button,
  Heading,
  HStack,
  Switch,
  Tabs,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { FileTextIcon, LinkIcon, PaperplaneIcon } from "@navikt/aksel-icons";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { DescriptionRichtextContainer } from "@/components/redaksjoneltInnhold/DescriptionRichtextContainer";
import { RedaksjoneltInnholdTabTittel } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdTabTittel";
import { PortableText } from "@mr/frontend-common";
import { PortableTextFormEditor } from "../portableText/PortableTextEditor";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { useFieldArray, useFormContext } from "react-hook-form";

interface RedaksjoneltInnholdFormProps {
  path: string;
  description: string;
  tiltakstype?: TiltakstypeDto;
}

export function RedaksjoneltInnholdForm({
  path,
  description,
  tiltakstype,
}: RedaksjoneltInnholdFormProps) {
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
          <ForHvem tiltakstype={tiltakstype} path={path} />
        </Tabs.Panel>
        <Tabs.Panel value="detaljer_og_innhold">
          <DetaljerOgInnhold tiltakstype={tiltakstype} path={path} />
        </Tabs.Panel>
        <Tabs.Panel value="pamelding_og_varighet">
          <PameldingOgVarighet tiltakstype={tiltakstype} path={path} />
        </Tabs.Panel>
        <Tabs.Panel value="kontaktinfo">
          <Kontaktinfo path={path} />
        </Tabs.Panel>
        <Tabs.Panel value="lenker">
          <RedaksjoneltInnholdLenkerForm path={path} />
        </Tabs.Panel>
        <Tabs.Panel value="del_med_bruker">
          <DelMedBruker tiltakstype={tiltakstype} path={path} />
        </Tabs.Panel>
      </Tabs>
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
  const { control, register } = useFormContext();
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
              <TextField
                size="small"
                label="Lenkenavn"
                {...register(fp(`faneinnhold.lenker.${index}.lenkenavn`))}
              />
              <TextField
                size="small"
                label="Lenke"
                {...register(fp(`faneinnhold.lenker.${index}.lenke`))}
              />
              <HStack gap="space-8">
                <Switch {...register(fp(`faneinnhold.lenker.${index}.apneINyFane`))}>
                  Åpne i ny fane
                </Switch>
                <Switch {...register(fp(`faneinnhold.lenker.${index}.visKunForVeileder`))}>
                  Vis kun i Modia
                </Switch>
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
