import { Alert, BodyLong, Heading, HStack, Tabs, Textarea } from "@navikt/ds-react";
import { PortableText } from "@portabletext/react";
import { EmbeddedTiltakstype, VeilederflateTiltakstype } from "mulighetsrommet-api-client";
import { useFormContext } from "react-hook-form";
import { useTiltakstypeFaneinnhold } from "../../api/tiltaksgjennomforing/useTiltakstypeFaneinnhold";
import { Separator } from "../detaljside/Metadata";
import { PortableTextEditor } from "../portableText/PortableTextEditor";
import skjemastyles from "../skjema/Skjema.module.scss";
import { Laster } from "../laster/Laster";
import React from "react";
import { InlineErrorBoundary } from "../../ErrorBoundary";

interface RedaksjoneltInnholdFormProps {
  tiltakstype: EmbeddedTiltakstype;
}

export function RedaksjoneltInnholdForm({ tiltakstype }: RedaksjoneltInnholdFormProps) {
  return (
    <InlineErrorBoundary>
      <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
        <RedaksjoneltInnhold tiltakstype={tiltakstype} />
      </React.Suspense>
    </InlineErrorBoundary>
  );
}

function RedaksjoneltInnhold({ tiltakstype }: { tiltakstype: EmbeddedTiltakstype }) {
  const { register } = useFormContext();
  const { data: tiltakstypeSanityData } = useTiltakstypeFaneinnhold(tiltakstype.id);

  return (
    <div className={skjemastyles.container}>
      <HStack justify="space-between" align="start" gap="2">
        <Alert size="small" variant="info">
          Ikke del personopplysninger i fritekstfeltene
        </Alert>
      </HStack>
      <div className={skjemastyles.red_innhold_container}>
        {tiltakstypeSanityData?.beskrivelse && (
          <>
            <Heading size="medium">Beskrivelse</Heading>
            <BodyLong className={skjemastyles.preWrap}>
              {tiltakstypeSanityData?.beskrivelse}
            </BodyLong>
          </>
        )}
        <Textarea
          {...register("beskrivelse")}
          description="Beskrivelse av formålet med tiltaksgjennomføringen."
          label="Beskrivelse"
        />
        <Heading size="medium">Faneinnhold</Heading>
        <Tabs size="small" defaultValue="for_hvem">
          <Tabs.List>
            <Tabs.Tab value="for_hvem" label="For hvem" />
            <Tabs.Tab value="detaljer_og_innhold" label="Detaljer og innhold" />
            <Tabs.Tab value="pamelding_og_varighet" label="Påmelding og varighet" />
            <Tabs.Tab value="kontaktinfo" label="Kontaktinfo" />
            <Tabs.Tab value="alle" label="Alle faner" />
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
          <Tabs.Panel value="alle">
            <>
              <ForHvem tiltakstype={tiltakstypeSanityData} />
              <DetaljerOgInnhold tiltakstype={tiltakstypeSanityData} />
              <PameldingOgVarighet tiltakstype={tiltakstypeSanityData} />
              <Kontaktinfo />
            </>
          </Tabs.Panel>
        </Tabs>
      </div>
    </div>
  );
}

const ForHvem = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { register } = useFormContext();

  return (
    <div className={skjemastyles.faneinnhold_container}>
      {tiltakstype?.faneinnhold?.forHvemInfoboks && (
        <Alert className={skjemastyles.preWrap} variant="info">
          {tiltakstype?.faneinnhold?.forHvemInfoboks}
        </Alert>
      )}
      <PortableText value={tiltakstype?.faneinnhold?.forHvem} />
      <Separator />

      <div className={skjemastyles.description_richtext_container}>
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
      </div>
    </div>
  );
};

const DetaljerOgInnhold = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { register } = useFormContext();

  return (
    <div className={skjemastyles.faneinnhold_container}>
      {tiltakstype?.faneinnhold?.detaljerOgInnholdInfoboks && (
        <Alert variant="info">{tiltakstype?.faneinnhold?.detaljerOgInnholdInfoboks}</Alert>
      )}
      <PortableText value={tiltakstype?.faneinnhold?.detaljerOgInnhold} />
      <Separator />

      <div className={skjemastyles.description_richtext_container}>
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
      </div>
    </div>
  );
};

const PameldingOgVarighet = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { register } = useFormContext();

  return (
    <div className={skjemastyles.faneinnhold_container}>
      {tiltakstype?.faneinnhold?.pameldingOgVarighetInfoboks && (
        <Alert variant="info">{tiltakstype?.faneinnhold?.pameldingOgVarighetInfoboks}</Alert>
      )}
      <PortableText value={tiltakstype?.faneinnhold?.pameldingOgVarighet} />
      <Separator />

      <div className={skjemastyles.description_richtext_container}>
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
      </div>
    </div>
  );
};

const Kontaktinfo = () => {
  const { register } = useFormContext();

  return (
    <div className={skjemastyles.faneinnhold_container}>
      <PortableTextEditor
        {...register("faneinnhold.kontaktinfo")}
        label="Kontaktinfo"
        description="Ekstra tekst om kontaktinfo."
      />
    </div>
  );
};
