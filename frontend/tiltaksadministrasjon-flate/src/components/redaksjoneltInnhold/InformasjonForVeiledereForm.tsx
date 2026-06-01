import { Heading, HelpText, HStack, Label, VStack } from "@navikt/ds-react";
import { GjennomforingKontaktpersonDto } from "@tiltaksadministrasjon/api-client";
import { useFormContext } from "react-hook-form";
import { Laster } from "../laster/Laster";
import React, { useState } from "react";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { useSokNavAnsatt } from "@/api/ansatt/useSokNavAnsatt";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { RedaksjoneltInnholdForm } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdForm";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormListInput } from "@/components/skjema/FormListInput";
import { FormCombobox } from "@/components/skjema/FormCombobox";
import { FormComboboxMulti } from "@/components/skjema/FormComboboxMulti";
import { GjennomforingFormValues } from "@/pages/gjennomforing/form/validation";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";

interface Props {
  tiltakId: string;
  regionerOptions: kontorOption[];
  kontorerOptions: kontorOption[];
  andreEnheterOptions: kontorOption[];
  kontaktpersonForm: boolean;
  lagredeKontaktpersoner?: GjennomforingKontaktpersonDto[];
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
  const tiltakstype = useTiltakstype(tiltakId);

  return (
    <InlineErrorBoundary>
      <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
        <TwoColumnGrid separator>
          <RedaksjoneltInnholdForm
            path="veilederinformasjon"
            description="Beskrivelse av formålet med tiltaksgjennomføringen."
            tiltakstype={tiltakstype}
          />
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
  lagredeKontaktpersoner: GjennomforingKontaktpersonDto[];
}) {
  return (
    <>
      <Heading size="medium" spacing level="3">
        Geografisk tilgjengelighet
      </Heading>
      <VStack gap="space-8">
        <FormComboboxMulti<GjennomforingFormValues>
          id="navRegioner"
          placeholder="Velg en"
          label={avtaletekster.navRegionerLabel}
          name="veilederinformasjon.navRegioner"
          options={regionerOptions}
        />
        <FormComboboxMulti<GjennomforingFormValues>
          id="navKontorer"
          selectAll
          placeholder="Velg en"
          label={
            <LabelWithHelpText label={avtaletekster.navEnheterLabel}>
              Bestemmer hvilke Nav-enheter som kan velges i gjennomføringene til avtalen.
            </LabelWithHelpText>
          }
          name="veilederinformasjon.navKontorer"
          options={kontorerOptions}
        />
        <FormComboboxMulti<GjennomforingFormValues>
          id="navAndreEnheter"
          selectAll
          placeholder="Velg en (valgfritt)"
          label={
            <LabelWithHelpText label={avtaletekster.navAndreEnheterLabel}>
              Bestemmer hvilke andre Nav-enheter som kan velges i gjennomføringene til avtalen.
            </LabelWithHelpText>
          }
          name="veilederinformasjon.navAndreEnheter"
          options={andreEnheterOptions}
        />
        {kontaktpersonForm && (
          <>
            <Separator />
            <HStack gap="space-8" align="center">
              <Label size="small">{gjennomforingTekster.kontaktpersonNav.mainLabel}</Label>
              <HelpText>
                Bestemmer kontaktperson som veilederene kan hendvende seg til for informasjon om
                gjennomføringen."
              </HelpText>
            </HStack>
            <FormListInput
              name="veilederinformasjon.kontaktpersoner"
              addButtonLabel="Legg til ny kontaktperson"
              emptyItem={{ navIdent: "", beskrivelse: "" }}
              renderItem={(index, id) => (
                <SokEtterKontaktperson
                  index={index}
                  id={id}
                  lagredeKontaktpersoner={lagredeKontaktpersoner}
                />
              )}
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
  lagredeKontaktpersoner: GjennomforingKontaktpersonDto[];
}) {
  const [kontaktpersonerQuery, setKontaktpersonerQuery] = useState<string>("");
  const { data: kontaktpersoner } = useSokNavAnsatt(kontaktpersonerQuery, id);
  const { watch } = useFormContext<GjennomforingFormValues>();

  const kontaktpersonerOption = (selectedIndex: number) => {
    const excludedKontaktpersoner = watch("veilederinformasjon.kontaktpersoner").map(
      (k) => k.navIdent,
    );

    const alleredeValgt = watch("veilederinformasjon.kontaktpersoner")
      .filter((_, i) => i === selectedIndex)
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
        ?.filter((kontaktperson) => !excludedKontaktpersoner.includes(kontaktperson.navIdent))
        .map((kontaktperson) => ({
          label: `${kontaktperson.fornavn} ${kontaktperson.etternavn} - ${kontaktperson.navIdent}`,
          value: kontaktperson.navIdent,
        })) ?? [];

    return [...alleredeValgt, ...options];
  };

  return (
    <>
      <FormCombobox<GjennomforingFormValues>
        placeholder="Søk etter kontaktperson"
        label={gjennomforingTekster.kontaktpersonNav.navnLabel}
        name={`veilederinformasjon.kontaktpersoner.${index}.navIdent`}
        onChange={(value) => setKontaktpersonerQuery(value)}
        options={kontaktpersonerOption(index)}
        filteredOptions={kontaktpersonerOption(index)}
      />
      <FormTextField<GjennomforingFormValues>
        name={`veilederinformasjon.kontaktpersoner.${index}.beskrivelse`}
        label={gjennomforingTekster.kontaktpersonNav.beskrivelseLabel}
        placeholder="Unngå personopplysninger"
        maxLength={67}
      />
    </>
  );
}
