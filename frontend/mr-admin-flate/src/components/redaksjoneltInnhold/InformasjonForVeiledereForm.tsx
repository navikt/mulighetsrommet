import {
  Alert,
  BodyLong,
  Button,
  Heading,
  HelpText,
  HStack,
  Label,
  Tabs,
  Textarea,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { PortableText } from "../portableText/PortableText";
import { GjennomforingKontaktperson } from "@mr/api-client-v2";
import { VeilederflateTiltakstype } from "@tiltaksadministrasjon/api-client";
import { useFieldArray, useFormContext } from "react-hook-form";
import { useTiltakstypeFaneinnhold } from "@/api/gjennomforing/useTiltakstypeFaneinnhold";
import { Separator } from "../detaljside/Metadata";
import { Laster } from "../laster/Laster";
import React, { useState } from "react";
import { FileTextIcon, LinkIcon, PaperplaneIcon, PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import { Lenker } from "../lenker/Lenker";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { DescriptionRichtextContainer } from "@/components/redaksjoneltInnhold/DescriptionRichtextContainer";
import { RedaksjoneltInnholdTabTittel } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdTabTittel";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { KontaktpersonButton } from "../kontaktperson/KontaktpersonButton";
import { useSokNavAnsatt } from "@/api/ansatt/useSokNavAnsatt";
import { InferredGjennomforingSchema } from "./GjennomforingSchema";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { PortableTextFormEditor } from "../portableText/PortableTextEditor2";

interface Props {
  tiltakId: string;
  regionerOptions: kontorOption[];
  kontorerOptions: kontorOption[];
  andreEnheterOptions: kontorOption[];
  kontaktpersonForm: boolean;
  lagredeKontaktpersoner?: GjennomforingKontaktperson[];
}

export type kontorOption = {
  value: string;
  label: string;
};

export function InformasjonForVeiledereForm({
  tiltakId,
  kontorerOptions,
  regionerOptions,
  andreEnheterOptions,
  kontaktpersonForm,
  lagredeKontaktpersoner = [],
}: Props) {
  return (
    <InlineErrorBoundary>
      <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
        <TwoColumnGrid separator>
          <RedaksjoneltInnholdForm tiltakId={tiltakId} />
          <RegionerOgEnheterOgKontaktpersoner
            regionerOptions={regionerOptions}
            kontorerOptions={kontorerOptions}
            andreEnheterOptions={andreEnheterOptions}
            kontaktpersonForm={kontaktpersonForm}
            lagredeKontaktpersoner={lagredeKontaktpersoner}
          />
        </TwoColumnGrid>
      </React.Suspense>
    </InlineErrorBoundary>
  );
}

export function RedaksjoneltInnholdForm({ tiltakId }: { tiltakId: string }) {
  const { register } = useFormContext();
  const { data: tiltakstypeSanityData } = useTiltakstypeFaneinnhold(tiltakId);

  return (
    <RedaksjoneltInnholdContainer>
      <Heading size="medium" spacing level="3">
        Redaksjonelt innhold
      </Heading>
      <Alert size="small" variant="info">
        Ikke del personopplysninger i fritekstfeltene
      </Alert>
      <Textarea
        {...register("beskrivelse")}
        description="Beskrivelse av formålet med tiltaksgjennomføringen."
        label="Beskrivelse"
      />
      {tiltakstypeSanityData.beskrivelse && (
        <>
          <Heading size="medium">Generell informasjon</Heading>
          <BodyLong style={{ whiteSpace: "pre-wrap" }}>
            {tiltakstypeSanityData.beskrivelse}
          </BodyLong>
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
          <ForHvem tiltakstype={tiltakstypeSanityData} />
        </Tabs.Panel>
        <Tabs.Panel value="detaljer_og_innhold">
          <DetaljerOgInnhold tiltakstype={tiltakstypeSanityData} />
        </Tabs.Panel>
        <Tabs.Panel value="pamelding_og_varighet">
          <PameldingOgVarighet tiltakstype={tiltakstypeSanityData} />
        </Tabs.Panel>
        <Tabs.Panel value="kontaktinfo">
          <Kontaktinfo />
        </Tabs.Panel>
        <Tabs.Panel value="lenker">
          <Lenker />
        </Tabs.Panel>
        <Tabs.Panel value="del_med_bruker">
          <DelMedBruker tiltakstype={tiltakstypeSanityData} />
        </Tabs.Panel>
      </Tabs>
    </RedaksjoneltInnholdContainer>
  );
}

const ForHvem = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { register } = useFormContext();

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
        <Textarea
          {...register("faneinnhold.forHvemInfoboks")}
          label="Fremhevet informasjon til veileder som legger seg i blå infoboks i fanen «For hvem»"
          description="Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne."
        />
        <PortableTextFormEditor
          name="faneinnhold.forHvem"
          label="For hvem"
          description="Beskrivelse av hvem tiltakstypen passer for. Husk å bruke et kort og konsist språk."
        />
      </DescriptionRichtextContainer>
    </VStack>
  );
};

const DetaljerOgInnhold = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { register } = useFormContext();

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
        <Textarea
          {...register("faneinnhold.detaljerOgInnholdInfoboks")}
          label="Fremhevet informasjon til veileder som legger seg i blå infoboks i fanen «Detaljer og innhold»"
          description="Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne."
        />
        <PortableTextFormEditor
          name="faneinnhold.detaljerOgInnhold"
          label="Detaljer og innhold"
          description="Beskrivelse av detaljer og innhold for tiltakstypen. Husk å bruke et kort og konsist språk."
        />
      </DescriptionRichtextContainer>
    </VStack>
  );
};

const PameldingOgVarighet = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { register } = useFormContext();

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
        <Textarea
          {...register("faneinnhold.pameldingOgVarighetInfoboks")}
          label="Fremhevet informasjon til veileder som legger seg i blå infoboks i fanen «Påmelding og varighet»"
          description="Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne."
        />
        <PortableTextFormEditor
          name="faneinnhold.pameldingOgVarighet"
          label="Påmelding og varighet"
          description="Beskrivelse av rutiner rundt påmelding og varighet i tiltaket. Husk å bruke et kort og konsist språk."
        />
      </DescriptionRichtextContainer>
    </VStack>
  );
};

const Kontaktinfo = () => {
  const { register } = useFormContext();

  return (
    <VStack className="mt-4">
      <VStack gap="5">
        <Textarea
          {...register("faneinnhold.kontaktinfoInfoboks")}
          label="Fremhevet informasjon til veileder som legger seg i blå infoboks i fanen «Kontaktinfo»"
          description="Bruk denne tekstboksen for informasjon som skal være ekstra fremtredende for veilederne."
        />
        <PortableTextFormEditor
          name="faneinnhold.kontaktinfo"
          label="Kontaktinfo"
          description="Ekstra tekst om kontaktinfo."
        />
      </VStack>
    </VStack>
  );
};

const DelMedBruker = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { watch, setValue } = useFormContext();

  const [tekst, setTekst] = useState<string>(
    watch("faneinnhold.delMedBruker") ?? tiltakstype?.delingMedBruker ?? "",
  );

  function onChange(value: string) {
    if (value !== tiltakstype?.delingMedBruker) {
      setValue("faneinnhold.delMedBruker", value);
    }
  }

  return (
    <VStack className="mt-4">
      <Textarea
        onChange={(e) => {
          onChange(e.target.value);
          setTekst(e.target.value);
        }}
        value={tekst}
        label="Del med bruker"
        description="Bruk denne tekstboksen for å redigere teksten som sendes til bruker når man deler et tiltak. Det blir automatisk lagt til en ”Hei” og en “Hilsen”."
      />
    </VStack>
  );
};

function RegionerOgEnheterOgKontaktpersoner({
  regionerOptions,
  kontorerOptions,
  andreEnheterOptions,
  kontaktpersonForm,
  lagredeKontaktpersoner,
}: {
  regionerOptions: kontorOption[];
  kontorerOptions: kontorOption[];
  andreEnheterOptions: kontorOption[];
  kontaktpersonForm: boolean;
  lagredeKontaktpersoner: GjennomforingKontaktperson[];
}) {
  const { register, control } = useFormContext();

  const {
    fields: kontaktpersonFields,
    append: appendKontaktperson,
    remove: removeKontaktperson,
  } = useFieldArray({
    name: "kontaktpersoner",
    control,
  });

  return (
    <>
      <Heading size="medium" spacing level="3">
        Geografisk tilgjengelighet
      </Heading>
      <VStack gap="2">
        <ControlledMultiSelect
          inputId={"navRegioner"}
          size="small"
          placeholder="Velg en"
          label={avtaletekster.navRegionerLabel}
          {...register("navRegioner")}
          name={"navRegioner"}
          options={regionerOptions}
        />
        <ControlledMultiSelect
          inputId={"navKontorer"}
          size="small"
          velgAlle
          placeholder="Velg en"
          label={avtaletekster.navEnheterLabel}
          helpText="Bestemmer hvilke Nav-enheter som kan velges i gjennomføringene til avtalen."
          {...register("navKontorer")}
          options={kontorerOptions}
        />
        <ControlledMultiSelect
          inputId={"navAndreEnheter"}
          size="small"
          velgAlle
          placeholder="Velg en (valgfritt)"
          label={avtaletekster.navAndreEnheterLabel}
          helpText="Bestemmer hvilke andre Nav-enheter som kan velges i gjennomføringene til avtalen."
          {...register("navEnheterAndre")}
          options={andreEnheterOptions}
        />
        {kontaktpersonForm && (
          <>
            <Separator />
            <HStack gap="2" align="center">
              <Label size="small">{gjennomforingTekster.kontaktpersonNav.mainLabel}</Label>
              <HelpText>
                Bestemmer kontaktperson som veilederene kan hendvende seg til for informasjon om
                gjennomføringen."
              </HelpText>
            </HStack>
            {kontaktpersonFields.map((field, index) => {
              return (
                <div
                  className="bg-surface-subtle mt-4 p-2 relative border border-border-divider rounded"
                  key={field.id}
                >
                  <Button
                    className="p-0 float-right"
                    variant="tertiary"
                    size="small"
                    type="button"
                    onClick={() => removeKontaktperson(index)}
                  >
                    <XMarkIcon fontSize="1.5rem" />
                  </Button>
                  <div className="flex flex-col gap-4">
                    <SokEtterKontaktperson
                      index={index}
                      id={field.id}
                      lagredeKontaktpersoner={lagredeKontaktpersoner}
                    />
                  </div>
                </div>
              );
            })}
            <KontaktpersonButton
              onClick={() => appendKontaktperson({ navIdent: "", beskrivelse: "" })}
              knappetekst={
                <div className="flex items-center gap-2">
                  <PlusIcon aria-label="Legg til ny kontaktperson" />
                  Legg til ny kontaktperson
                </div>
              }
            />
          </>
        )}
      </VStack>
    </>
  );
}

function SokEtterKontaktperson({
  index,
  id,
  lagredeKontaktpersoner,
}: {
  index: number;
  id: string;
  lagredeKontaktpersoner: GjennomforingKontaktperson[];
}) {
  const [kontaktpersonerQuery, setKontaktpersonerQuery] = useState<string>("");
  const { data: kontaktpersoner } = useSokNavAnsatt(kontaktpersonerQuery, id);
  const { register, watch } = useFormContext<InferredGjennomforingSchema>();

  const kontaktpersonerOption = (selectedIndex: number) => {
    const excludedKontaktpersoner = watch("kontaktpersoner")?.map((k) => k.navIdent);

    const alleredeValgt = watch("kontaktpersoner")
      ?.filter((_, i) => i === selectedIndex)
      .map((kontaktperson) => {
        const personFraSok = kontaktpersoner?.find((k) => k.navIdent === kontaktperson.navIdent);
        const personFraDb = lagredeKontaktpersoner.find(
          (k) => k.navIdent === kontaktperson.navIdent,
        );
        const navn = personFraSok
          ? `${personFraSok.fornavn} ${personFraSok.etternavn}`
          : personFraDb?.navn;

        return {
          label: navn ? `${navn} - ${kontaktperson.navIdent}` : kontaktperson.navIdent,
          value: kontaktperson.navIdent,
        };
      });

    const options =
      kontaktpersoner
        ?.filter((kontaktperson) => !excludedKontaktpersoner?.includes(kontaktperson.navIdent))
        .map((kontaktperson) => ({
          label: `${kontaktperson.fornavn} ${kontaktperson.etternavn} - ${kontaktperson.navIdent}`,
          value: kontaktperson.navIdent,
        })) ?? [];

    return alleredeValgt ? [...alleredeValgt, ...options] : options;
  };

  return (
    <>
      <ControlledSokeSelect
        size="small"
        placeholder="Søk etter kontaktperson"
        label={gjennomforingTekster.kontaktpersonNav.navnLabel}
        {...register(`kontaktpersoner.${index}.navIdent`, {
          shouldUnregister: true,
        })}
        onInputChange={setKontaktpersonerQuery}
        options={kontaktpersonerOption(index)}
      />
      <TextField
        size="small"
        label={gjennomforingTekster.kontaktpersonNav.beskrivelseLabel}
        placeholder="Unngå personopplysninger"
        maxLength={67}
        {...register(`kontaktpersoner.${index}.beskrivelse`, {
          shouldUnregister: true,
        })}
      />
    </>
  );
}
