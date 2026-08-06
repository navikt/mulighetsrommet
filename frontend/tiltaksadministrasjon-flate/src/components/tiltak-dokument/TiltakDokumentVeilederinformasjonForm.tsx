import { Heading, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import { TiltakDokumentFormValues } from "@/pages/tiltak-dokument/TiltakDokumentFormValues";
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

export function TiltakDokumentVeilederinformasjonForm() {
  const { watch } = useFormContext<TiltakDokumentFormValues>();

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
        <RedaksjoneltInnholdForm
          path="veilederinformasjon"
          description="Beskrivelse av formålet med gjennomføringen."
        />
      </VStack>
      <VStack gap="space-16">
        <Heading size="small" level="3">
          Geografisk tilgjengelighet
        </Heading>

        <FormComboboxMulti<TiltakDokumentFormValues>
          name="veilederinformasjon.navRegioner"
          label={avtaletekster.navRegionerLabel}
          placeholder="Velg en"
          options={regionOptions}
        />

        <FormComboboxMulti<TiltakDokumentFormValues>
          name="veilederinformasjon.navKontorer"
          selectAll
          label={avtaletekster.navEnheterLabel}
          placeholder="Velg en"
          options={kontorOptions}
        />

        <FormComboboxMulti<TiltakDokumentFormValues>
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
          renderItem={(index, id) => <NavKontaktpersonFields index={index} id={id} />}
        />
      </VStack>
    </TwoColumnGrid>
  );
}

function NavKontaktpersonFields({ index, id }: { index: number; id: string }) {
  const [query, setQuery] = useState("");
  const { data: ansatte } = useSokNavAnsatt(query, id);
  const { watch } = useFormContext<TiltakDokumentFormValues>();

  const kontaktpersoner = watch("veilederinformasjon.kontaktpersoner");
  const excludedIdenter = kontaktpersoner.map((k) => k.navIdent);

  const alleredeValgt = kontaktpersoner
    .filter((_, i) => i === index)
    .map((k) => {
      const fraSok = ansatte?.find((a) => a.navIdent === k.navIdent);
      const navn = fraSok ? `${fraSok.fornavn} ${fraSok.etternavn}` : k.navIdent;
      return { label: navn ? `${navn} - ${k.navIdent}` : k.navIdent, value: k.navIdent };
    });

  const options =
    ansatte
      ?.filter((a) => !excludedIdenter.includes(a.navIdent))
      .map((a) => ({
        label: `${a.fornavn} ${a.etternavn} - ${a.navIdent}`,
        value: a.navIdent,
      })) ?? [];

  return (
    <>
      <FormCombobox<TiltakDokumentFormValues>
        placeholder="Søk etter kontaktperson"
        label={gjennomforingTekster.kontaktpersonNav.navnLabel}
        name={`veilederinformasjon.kontaktpersoner.${index}.navIdent`}
        onChange={setQuery}
        options={[...alleredeValgt, ...options]}
        filteredOptions={[...alleredeValgt, ...options]}
      />
      <FormTextField<TiltakDokumentFormValues>
        name={`veilederinformasjon.kontaktpersoner.${index}.beskrivelse`}
        label={gjennomforingTekster.kontaktpersonNav.beskrivelseLabel}
        placeholder="Unngå personopplysninger"
        maxLength={67}
      />
    </>
  );
}
