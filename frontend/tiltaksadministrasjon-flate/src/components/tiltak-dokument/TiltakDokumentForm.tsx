import { Heading, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { useRef, useState } from "react";
import { TiltakDokumentFormValues } from "@/pages/tiltak-dokument/TiltakDokumentFormValues";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { FormCombobox } from "@/components/skjema/FormCombobox";
import { FormComboboxMulti } from "@/components/skjema/FormComboboxMulti";
import { FormListInput } from "@/components/skjema/FormListInput";
import { useNavAnsatte } from "@/api/ansatt/useNavAnsatte";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { useKontorstruktur } from "@/api/enhet/useKontorstruktur";
import { useTiltakstyperForGjennomforinger } from "@/api/tiltakstyper/useTiltakstyperForGjennomforinger";
import { useSokNavAnsatt } from "@/api/ansatt/useSokNavAnsatt";
import { ArrangorKontaktpersonAnsvar, Rolle } from "@tiltaksadministrasjon/api-client";
import {
  getLokaleUnderenheterAsSelectOptions,
  getAndreUnderenheterAsSelectOptions,
} from "@/api/enhet/helpers";
import { RedaksjoneltInnholdForm } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdForm";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { HStack, Label, HelpText } from "@navikt/ds-react";
import { useDebounce } from "@mr/frontend-common";
import { ArrangorKontaktpersonerModal } from "@/components/arrangor/ArrangorKontaktpersonerModal";
import { KontaktpersonButton } from "@/components/kontaktperson/KontaktpersonButton";
import { FormSelect } from "../skjema/FormSelect";

export function TiltakDokumentForm() {
  const { watch, setValue } = useFormContext<TiltakDokumentFormValues>();
  const arrangorKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const tiltakstyper = useTiltakstyperForGjennomforinger();
  const tiltakstypeOptions = tiltakstyper.map((t) => ({ value: t.id, label: t.navn }));

  const { data: administratorer } = useNavAnsatte([Rolle.TILTAKSGJENNOMFORINGER_SKRIV]);
  const administratorOptions = administratorer.map((a) => ({
    value: a.navIdent,
    label: `${a.fornavn} ${a.etternavn} - ${a.navIdent}`,
  }));

  const [arrangorQuery, setArrangorQuery] = useState("");
  const debouncedArrangorQuery = useDebounce(arrangorQuery, 300);
  const { data: arrangorerResult } = useArrangorer(undefined, { sok: debouncedArrangorQuery });
  const arrangorOptions =
    arrangorerResult?.data.map((a) => ({
      value: a.id,
      label: `${a.navn} - ${a.organisasjonsnummer}`,
    })) ?? [];

  const arrangorId = watch("arrangorId");
  const { data: arrangorKontaktpersoner } = useArrangorKontaktpersoner(arrangorId ?? "");
  const arrangorKontaktpersonOptions = (arrangorKontaktpersoner ?? [])
    .filter((kp) => kp.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.GJENNOMFORING))
    .map((kp) => ({ value: kp.id, label: kp.navn }));

  const { data: kontorstruktur } = useKontorstruktur();
  const regionOptions = kontorstruktur.map((k) => ({
    value: k.region.enhetsnummer,
    label: k.region.navn,
  }));
  const navRegioner = watch("veilederinformasjon.navRegioner");
  const kontorOptions = getLokaleUnderenheterAsSelectOptions(navRegioner, kontorstruktur);
  const andreEnheterOptions = getAndreUnderenheterAsSelectOptions(navRegioner, kontorstruktur);

  return (
    <VStack gap="space-16">
      <FormTextField<TiltakDokumentFormValues> name="navn" label="Navn" required />

      <FormSelect<TiltakDokumentFormValues> name="tiltakstypeId" label="Tiltakstype">
        <option value="">-- Velg en --</option>
        {tiltakstypeOptions.map((type) => (
          <option key={type.value} value={type.value}>
            {type.label}
          </option>
        ))}
      </FormSelect>

      <FormTextarea<TiltakDokumentFormValues>
        name="stedForGjennomforing"
        label="Sted for gjennomføring (valgfritt)"
        minRows={2}
        maxRows={4}
      />

      <FormComboboxMulti<TiltakDokumentFormValues>
        name="administratorer"
        label={
          <LabelWithHelpText label={gjennomforingTekster.administratorerForGjennomforingenLabel}>
            Bestemmer hvem som eier gjennomføringen.
          </LabelWithHelpText>
        }
        placeholder="Administratorer"
        options={administratorOptions}
      />

      <Separator />
      <Heading size="small" level="3">
        Arrangør (valgfritt)
      </Heading>

      <FormCombobox<TiltakDokumentFormValues>
        name="arrangorId"
        label={gjennomforingTekster.tiltaksarrangorUnderenhetLabel}
        placeholder="Søk etter arrangør"
        options={arrangorOptions}
        onChange={setArrangorQuery}
        filteredOptions={arrangorOptions}
      />

      <VStack>
        <FormComboboxMulti<TiltakDokumentFormValues>
          name="arrangorKontaktpersoner"
          label={gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
          readOnly={!arrangorId}
          placeholder="Velg kontaktpersoner"
          options={arrangorKontaktpersonOptions}
        />
        <KontaktpersonButton
          onClick={() => arrangorKontaktpersonerModalRef.current?.showModal()}
          knappetekst="Opprett eller rediger kontaktpersoner"
        />
      </VStack>

      {arrangorId && (
        <ArrangorKontaktpersonerModal
          arrangorId={arrangorId}
          modalRef={arrangorKontaktpersonerModalRef}
          onOpprettSuccess={(kontaktperson) => {
            if (!kontaktperson.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.GJENNOMFORING)) {
              return;
            }
            const kontaktpersoner = watch("arrangorKontaktpersoner");
            setValue("arrangorKontaktpersoner", [
              ...kontaktpersoner.filter((k) => k !== kontaktperson.id),
              kontaktperson.id,
            ]);
          }}
        />
      )}

      <Separator />
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
      <HStack gap="space-8" align="center">
        <Label size="small">{gjennomforingTekster.kontaktpersonNav.mainLabel}</Label>
        <HelpText>
          Bestemmer kontaktperson som veilederne kan henvende seg til for informasjon om
          gjennomføringen.
        </HelpText>
      </HStack>

      <FormListInput
        name="veilederinformasjon.kontaktpersoner"
        addButtonLabel="Legg til ny kontaktperson"
        emptyItem={{ navIdent: "", beskrivelse: "" }}
        renderItem={(index, id) => <NavKontaktpersonFields index={index} id={id} />}
      />

      <Separator />
      <RedaksjoneltInnholdForm
        path="veilederinformasjon"
        description="Beskrivelse av formålet med gjennomføringen."
      />
    </VStack>
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
