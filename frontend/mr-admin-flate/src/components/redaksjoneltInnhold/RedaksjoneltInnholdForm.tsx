import {
  Alert,
  BodyLong,
  Heading,
  HStack,
  Tabs,
  Textarea,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { PortableText } from "@portabletext/react";
import { EmbeddedTiltakstype, VeilederflateTiltakstype } from "@mr/api-client";
import { FieldError, FieldErrorsImpl, Merge, useFormContext } from "react-hook-form";
import { useTiltakstypeFaneinnhold } from "@/api/tiltaksgjennomforing/useTiltakstypeFaneinnhold";
import { Separator } from "../detaljside/Metadata";
import { PortableTextEditor } from "../portableText/PortableTextEditor";
import { Laster } from "../laster/Laster";
import React, { useState } from "react";
import { FileTextIcon, LinkIcon, PaperplaneIcon } from "@navikt/aksel-icons";
import { Lenker } from "../lenker/Lenker";
import { InlineErrorBoundary } from "@mr/frontend-common";
import { InferredFaneinnholdSchema } from "@/components/redaksjoneltInnhold/FaneinnholdSchema";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { SkjemaDetaljerContainer } from "@/components/skjema/SkjemaDetaljerContainer";
import { FaneinnholdContainer } from "@/components/redaksjoneltInnhold/FaneinnholdContainer";
import { DescriptionRichtextContainer } from "@/components/redaksjoneltInnhold/DescriptionRichtextContainer";
import { RedaksjoneltInnholdTabTittel } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdTabTittel";
import { isKursTiltak } from "@mr/frontend-common/utils/utils";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";

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
  const {
    register,
    formState: { errors },
  } = useFormContext();
  const { data: tiltakstypeSanityData } = useTiltakstypeFaneinnhold(tiltakstype.id);

  return (
    <SkjemaDetaljerContainer>
      <HStack justify="space-between" align="start" gap="2">
        <Alert size="small" variant="info">
          Ikke del personopplysninger i fritekstfeltene
        </Alert>
      </HStack>
      <RedaksjoneltInnholdContainer>
        {isKursTiltak(tiltakstype.tiltakskode) && (
          <TextField
            size="small"
            error={
              (
                errors.faneinnhold as Merge<
                  FieldError,
                  FieldErrorsImpl<NonNullable<InferredFaneinnholdSchema>>
                >
              )?.kurstittel?.message as string
            }
            {...register("faneinnhold.kurstittel")}
            label={
              <LabelWithHelpText label="Kurstittel" helpTextTitle="Hjelpetekst">
                Brukerrettet kurstittel som vises i aktivitetsplanen og vedtak. Gi en kort og presis
                beskrivelse av kurset. Unngå å nevne tiltakstype eller annen informasjon som ikke er
                relevant for brukeren. For eksempel: "Sveisekurs", "Truckførerkurs" eller
                "Arbeidsnorsk med praksis"
              </LabelWithHelpText>
            }
          />
        )}
        {tiltakstypeSanityData?.beskrivelse && (
          <>
            <Heading size="medium">Beskrivelse</Heading>
            <BodyLong style={{ whiteSpace: "pre-wrap" }}>
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
    </SkjemaDetaljerContainer>
  );
}

const ForHvem = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { register } = useFormContext();

  return (
    <FaneinnholdContainer>
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
    </FaneinnholdContainer>
  );
};

const DetaljerOgInnhold = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { register } = useFormContext();

  return (
    <FaneinnholdContainer>
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
    </FaneinnholdContainer>
  );
};

const PameldingOgVarighet = ({ tiltakstype }: { tiltakstype?: VeilederflateTiltakstype }) => {
  const { register } = useFormContext();

  return (
    <FaneinnholdContainer>
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
    </FaneinnholdContainer>
  );
};

const Kontaktinfo = () => {
  const { register } = useFormContext();

  return (
    <FaneinnholdContainer>
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
    </FaneinnholdContainer>
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
    <FaneinnholdContainer>
      <Textarea
        onChange={(e) => {
          onChange(e.target.value);
          setTekst(e.target.value);
        }}
        value={tekst}
        label="Del med bruker"
        description="Bruk denne tekstboksen for å redigere teksten som sendes til bruker når man deler et tiltak. Det blir automatisk lagt til en ”Hei” og en “Hilsen”."
      />
    </FaneinnholdContainer>
  );
};
