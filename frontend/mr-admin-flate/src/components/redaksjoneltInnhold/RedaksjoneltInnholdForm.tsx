import { Alert, BodyLong, Heading, Tabs, Textarea, VStack } from "@navikt/ds-react";
import { PortableText } from "@portabletext/react";
import { EmbeddedTiltakstype, NavEnhetDto, VeilederflateTiltakstype } from "@mr/api-client-v2";
import { useFormContext } from "react-hook-form";
import { useTiltakstypeFaneinnhold } from "@/api/gjennomforing/useTiltakstypeFaneinnhold";
import { Separator } from "../detaljside/Metadata";
import { PortableTextEditor } from "../portableText/PortableTextEditor";
import { Laster } from "../laster/Laster";
import React, { useState } from "react";
import { FileTextIcon, LinkIcon, PaperplaneIcon } from "@navikt/aksel-icons";
import { Lenker } from "../lenker/Lenker";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { DescriptionRichtextContainer } from "@/components/redaksjoneltInnhold/DescriptionRichtextContainer";
import { RedaksjoneltInnholdTabTittel } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdTabTittel";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { getLokaleUnderenheterAsSelectOptions } from "@/api/enhet/helpers";
import { SelectOption } from "@mr/frontend-common/components/SokeSelect";
import { MultiValue } from "react-select";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";

interface RedaksjoneltInnholdFormProps {
  tiltakstype: EmbeddedTiltakstype;
  regionerOptions: kontorOption[];
  kontorerOptions: kontorOption[];
  andreEnheterOptions: kontorOption[];
}

export type kontorOption = {
  value: string;
  label: string;
};

export function RedaksjoneltInnholdForm({
  tiltakstype,
  kontorerOptions,
  regionerOptions,
  andreEnheterOptions,
}: RedaksjoneltInnholdFormProps) {
  return (
    <InlineErrorBoundary>
      <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
        <RedaksjoneltInnhold
          tiltakstype={tiltakstype}
          regionerOptions={regionerOptions}
          kontorerOptions={kontorerOptions}
          andreEnheterOptions={andreEnheterOptions}
        />
      </React.Suspense>
    </InlineErrorBoundary>
  );
}

function RedaksjoneltInnhold({
  tiltakstype,
  regionerOptions,
  kontorerOptions,
  andreEnheterOptions,
}: RedaksjoneltInnholdFormProps) {
  const { register } = useFormContext();
  const { data: tiltakstypeSanityData } = useTiltakstypeFaneinnhold(tiltakstype.id);

  return (
    <TwoColumnGrid separator>
      <RedaksjoneltInnholdContainer>
        <Alert size="small" variant="info">
          Ikke del personopplysninger i fritekstfeltene
        </Alert>
        <Textarea
          {...register("beskrivelse")}
          description="Beskrivelse av formålet med tiltaksgjennomføringen."
          label="Beskrivelse"
        />
        {tiltakstypeSanityData?.beskrivelse && (
          <>
            <Heading size="medium">Generell informasjon</Heading>
            <BodyLong style={{ whiteSpace: "pre-wrap" }}>
              {tiltakstypeSanityData?.beskrivelse}
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

      <RegionerOgEnheter
        regionerOptions={regionerOptions}
        kontorerOptions={kontorerOptions}
        andreEnheterOptions={andreEnheterOptions}
      />
    </TwoColumnGrid>
  );
}

const ForHvem = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { register } = useFormContext();

  return (
    <VStack className="mt-4">
      {tiltakstype?.faneinnhold?.forHvemInfoboks && (
        <Alert style={{ whiteSpace: "pre-wrap" }} variant="info">
          {tiltakstype?.faneinnhold?.forHvemInfoboks}
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
        <PortableTextEditor
          {...register("faneinnhold.forHvem")}
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
        <Alert variant="info">{tiltakstype?.faneinnhold?.detaljerOgInnholdInfoboks}</Alert>
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
        <PortableTextEditor
          {...register("faneinnhold.detaljerOgInnhold")}
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
        <Alert variant="info">{tiltakstype?.faneinnhold?.pameldingOgVarighetInfoboks}</Alert>
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
        <PortableTextEditor
          {...register("faneinnhold.pameldingOgVarighet")}
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
        <PortableTextEditor
          {...register("faneinnhold.kontaktinfo")}
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

function velgAlleLokaleUnderenheter(
  selectedOptions: MultiValue<SelectOption<string>>,
  enheter: NavEnhetDto[],
): string[] {
  const regioner = selectedOptions?.map((option) => option.value);
  return getLokaleUnderenheterAsSelectOptions(regioner, enheter).map((option) => option.value);
}

function RegionerOgEnheter({
  regionerOptions,
  kontorerOptions,
  andreEnheterOptions,
}: {
  regionerOptions: kontorOption[];
  kontorerOptions: kontorOption[];
  andreEnheterOptions: kontorOption[];
}) {
  const { register, setValue, watch } = useFormContext();
  const { data: enheter } = useNavEnheter();

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
          additionalOnChange={(selectedOptions) => {
            if ((watch("navRegioner")?.length ?? 0) > 1) {
              const alleLokaleUnderenheter = velgAlleLokaleUnderenheter(selectedOptions, enheter);
              setValue("navKontorer", alleLokaleUnderenheter as [string, ...string[]]);
            } else {
              const alleLokaleUnderenheter = velgAlleLokaleUnderenheter(selectedOptions, enheter);
              const navKontorer = watch("navKontorer")?.filter((enhet: string) =>
                alleLokaleUnderenheter.includes(enhet ?? ""),
              );
              setValue("navKontorer", navKontorer as [string, ...string[]]);
            }
          }}
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
          {...register("navAndreEnheter")}
          options={andreEnheterOptions}
        />
      </VStack>
    </>
  );
}
