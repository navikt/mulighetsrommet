import { Heading, VStack } from "@navikt/ds-react";
import { Suspense, useState } from "react";
import { useFormContext } from "react-hook-form";
import { TiltakBeskrivelseFormValues } from "@/pages/tiltak-beskrivelse/TiltakBeskrivelseFormValues";
import { FormCombobox } from "@/components/skjema/FormCombobox";
import { FormComboboxMulti } from "@/components/skjema/FormComboboxMulti";
import { FormListInput } from "@/components/skjema/FormListInput";
import { FormTextField } from "@/components/skjema/FormTextField";
import { useKontorstruktur } from "@/api/enhet/useKontorstruktur";
import { useSokNavAnsatt } from "@/api/ansatt/useSokNavAnsatt";
import {
  getLokaleUnderenheterAsSelectOptions,
  getAndreUnderenheterAsSelectOptions,
} from "@/api/enhet/helpers";
import { RedaksjoneltInnholdForm } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdForm";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { Laster } from "@/components/laster/Laster";
import {
  formaterNavn,
  getNavKontaktpersonOptions,
} from "@/components/kontaktperson/navKontaktpersonOptions";

interface Props {
  lagredeKontaktpersoner?: { navIdent: string; navn: string }[];
}

export function TiltakBeskrivelseVeilederinformasjonForm({ lagredeKontaktpersoner = [] }: Props) {
  const { watch } = useFormContext<TiltakBeskrivelseFormValues>();

  const { data: kontorstruktur } = useKontorstruktur();
  const regionOptions = kontorstruktur.map((k) => ({
    value: k.region.enhetsnummer,
    label: k.region.navn,
  }));
  const navRegioner = watch("veilederinformasjon.navRegioner");
  const kontorOptions = getLokaleUnderenheterAsSelectOptions(navRegioner, kontorstruktur);
  const andreEnheterOptions = getAndreUnderenheterAsSelectOptions(navRegioner, kontorstruktur);

  return (
    <TwoColumnGrid>
      <VStack gap="space-16">
        <InlineErrorBoundary>
          <Suspense fallback={<Laster tekst="Laster innhold" />}>
            <RedaksjoneltInnholdFormMedTiltakstype />
          </Suspense>
        </InlineErrorBoundary>
      </VStack>
      <VStack gap="space-16">
        <Heading size="small" level="3">
          Geografisk tilgjengelighet
        </Heading>

        <FormComboboxMulti<TiltakBeskrivelseFormValues>
          name="veilederinformasjon.navRegioner"
          label={avtaletekster.navRegionerLabel}
          placeholder="Velg en"
          options={regionOptions}
        />

        <FormComboboxMulti<TiltakBeskrivelseFormValues>
          name="veilederinformasjon.navKontorer"
          selectAll
          label={avtaletekster.navEnheterLabel}
          placeholder="Velg en"
          options={kontorOptions}
        />

        <FormComboboxMulti<TiltakBeskrivelseFormValues>
          name="veilederinformasjon.navAndreEnheter"
          selectAll
          label={avtaletekster.navAndreEnheterLabel}
          placeholder="Velg en (valgfritt)"
          options={andreEnheterOptions}
        />

        <Separator />
        <Heading size="small" level="3">
          {gjennomforingTekster.kontaktpersonNav.mainLabel}
        </Heading>

        <FormListInput
          name="veilederinformasjon.kontaktpersoner"
          addButtonLabel="Legg til ny kontaktperson"
          emptyItem={{ navIdent: "", beskrivelse: "" }}
          renderItem={(index, id) => (
            <NavKontaktpersonFields
              index={index}
              id={id}
              lagredeKontaktpersoner={lagredeKontaktpersoner}
            />
          )}
        />
      </VStack>
    </TwoColumnGrid>
  );
}

function RedaksjoneltInnholdFormMedTiltakstype() {
  const { watch } = useFormContext<TiltakBeskrivelseFormValues>();
  const tiltakstypeId = watch("tiltakstypeId");

  if (!tiltakstypeId) {
    return (
      <RedaksjoneltInnholdForm
        path="veilederinformasjon"
        description="Beskrivelse av formålet med gjennomføringen."
      />
    );
  }

  return <RedaksjoneltInnholdFormInner tiltakstypeId={tiltakstypeId} />;
}

function RedaksjoneltInnholdFormInner({ tiltakstypeId }: { tiltakstypeId: string }) {
  const tiltakstype = useTiltakstype(tiltakstypeId);
  return (
    <RedaksjoneltInnholdForm
      path="veilederinformasjon"
      description="Beskrivelse av formålet med gjennomføringen."
      tiltakstype={tiltakstype}
    />
  );
}

function NavKontaktpersonFields({
  index,
  id,
  lagredeKontaktpersoner,
}: {
  index: number;
  id: string;
  lagredeKontaktpersoner: { navIdent: string; navn: string }[];
}) {
  const [query, setQuery] = useState("");
  const [kjenteNavn, setKjenteNavn] = useState<Record<string, string>>(() =>
    Object.fromEntries(lagredeKontaktpersoner.map((k) => [k.navIdent, k.navn])),
  );
  const { data: ansatte } = useSokNavAnsatt(query, id);
  const { watch } = useFormContext<TiltakBeskrivelseFormValues>();

  const kontaktpersoner = watch("veilederinformasjon.kontaktpersoner");
  const valgtNavIdent = kontaktpersoner[index]?.navIdent;
  const alleValgteNavIdenter = kontaktpersoner.map((k) => k.navIdent);
  const kontaktpersonerOptions = getNavKontaktpersonOptions({
    valgtNavIdent,
    alleValgteNavIdenter,
    sokeresultat: ansatte,
    kjenteNavn,
  });

  return (
    <>
      <FormCombobox<TiltakBeskrivelseFormValues>
        placeholder="Søk etter kontaktperson"
        label={gjennomforingTekster.kontaktpersonNav.navnLabel}
        name={`veilederinformasjon.kontaktpersoner.${index}.navIdent`}
        onChange={setQuery}
        onToggleSelected={(navIdent, isSelected) => {
          if (!isSelected) return;
          const treff = ansatte?.find((k) => k.navIdent === navIdent);
          if (treff) setKjenteNavn((prev) => ({ ...prev, [navIdent]: formaterNavn(treff) }));
        }}
        options={kontaktpersonerOptions}
        filteredOptions={kontaktpersonerOptions}
      />
      <FormTextField<TiltakBeskrivelseFormValues>
        name={`veilederinformasjon.kontaktpersoner.${index}.beskrivelse`}
        label={gjennomforingTekster.kontaktpersonNav.beskrivelseLabel}
        placeholder="Unngå personopplysninger"
        maxLength={67}
      />
    </>
  );
}
