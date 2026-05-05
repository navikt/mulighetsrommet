import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { useSyncArrangorFromBrreg } from "@/api/arrangor/useSyncArrangorFromBrreg";
import { Alert, VStack } from "@navikt/ds-react";
import {
  ArrangorDto,
  ArrangorKontaktperson,
  ArrangorKontaktpersonAnsvar,
  BrregHovedenhetDto,
  BrregUnderenhetDto,
} from "@tiltaksadministrasjon/api-client";
import { useRef, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { ArrangorKontaktpersonerModal } from "@/components/arrangor/ArrangorKontaktpersonerModal";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { AvtaleFormValues } from "@/pages/avtaler/form/validation";
import { FormGroup } from "@/layouts/FormGroup";
import { KontaktpersonButton } from "@/components/kontaktperson/KontaktpersonButton";
import { useSokBrregHovedenhet } from "@/api/virksomhet/useSokBrregHovedenhet";
import { useBrregUnderenheter } from "@/api/virksomhet/useBrregUnderenheter";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { SelectOption } from "@mr/frontend-common/components/SokeSelect";
import { FormCombobox } from "@/components/skjema/FormCombobox";

export function AvtaleArrangorForm() {
  const arrangorKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);
  const [sokArrangor, setSokArrangor] = useState("");

  const { data: brregVirksomheter = [] } = useSokBrregHovedenhet(sokArrangor);

  const { watch, setValue } = useFormContext<DeepPartial<AvtaleFormValues>>();
  const watchedArrangor = watch("detaljer.arrangor.hovedenhet") ?? "";
  const watchedUnderenheter = (watch("detaljer.arrangor.underenheter") ?? []).filter(
    (o): o is string => typeof o === "string",
  );

  const { data: arrangor } = useSyncArrangorFromBrreg(watchedArrangor);
  const { data: underenheter } = useBrregUnderenheter(watchedArrangor);
  const { data: kontaktpersoner } = useArrangorKontaktpersoner(arrangor?.id ?? "");

  const arrangorHovedenhetOptions = getArrangorHovedenhetOptions(brregVirksomheter, arrangor);
  const arrangorUnderenhetOptions = getArrangorUnderenhetOptions(
    underenheter ?? [],
    watchedUnderenheter,
  );
  const arrangorKontaktpersonOptions = getArrangorKontaktpersonOptions(kontaktpersoner ?? []);

  const underenheterIsEmpty = arrangorUnderenhetOptions.length === 0;

  return (
    <>
      <FormGroup>
        <FormCombobox<DeepPartial<AvtaleFormValues>>
          id="arrangorHovedenhet"
          name="detaljer.arrangor.hovedenhet"
          label={avtaletekster.tiltaksarrangorHovedenhetLabel}
          placeholder="Navn eller organisasjonsnummer for tiltaksarrangør"
          onChange={(value) => setSokArrangor(value)}
          filteredOptions={arrangorHovedenhetOptions}
          options={arrangorHovedenhetOptions}
          onToggleSelected={(_option, isSelected) => {
            if (!isSelected) {
              setValue("detaljer.arrangor.underenheter", []);
            }
          }}
        />
        {arrangor && underenheter && underenheterIsEmpty && (
          <Alert variant="warning">
            Bedriften {arrangor.navn} mangler underenheter i Brønnøysundregistrene og kan derfor
            ikke velges som tiltaksarrangør.
          </Alert>
        )}
        <FormCombobox<DeepPartial<AvtaleFormValues>>
          id="arrangorUnderenheter"
          name="detaljer.arrangor.underenheter"
          label={
            <LabelWithHelpText label={avtaletekster.tiltaksarrangorUnderenheterLabel}>
              Bestemmer hvilke arrangører som kan velges i gjennomføringene til avtalen.
            </LabelWithHelpText>
          }
          placeholder="Velg underenhet for tiltaksarrangør"
          isMultiSelect
          options={arrangorUnderenhetOptions}
          readOnly={underenheterIsEmpty}
        />
        <VStack>
          <FormCombobox<DeepPartial<AvtaleFormValues>>
            id="arrangorKontaktpersoner"
            name="detaljer.arrangor.kontaktpersoner"
            label={avtaletekster.kontaktpersonerHosTiltaksarrangorLabel}
            placeholder="Velg kontaktpersoner"
            isMultiSelect
            options={arrangorKontaktpersonOptions}
            readOnly={!arrangor}
          />
          <KontaktpersonButton
            onClick={() => arrangorKontaktpersonerModalRef.current?.showModal()}
            knappetekst="Opprett eller rediger kontaktpersoner"
          />
        </VStack>
      </FormGroup>
      {arrangor && (
        <ArrangorKontaktpersonerModal
          arrangorId={arrangor.id}
          modalRef={arrangorKontaktpersonerModalRef}
          onOpprettSuccess={(kontaktperson) => {
            if (!kontaktperson.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.AVTALE)) {
              return;
            }

            const kontaktpersoner = watch("detaljer.arrangor.kontaktpersoner") ?? [];
            setValue("detaljer.arrangor.kontaktpersoner", [
              ...kontaktpersoner.filter((k) => k !== kontaktperson.id),
              kontaktperson.id,
            ]);
          }}
        />
      )}
    </>
  );
}

function getArrangorHovedenhetOptions(
  virksomheter: BrregHovedenhetDto[],
  arrangor: ArrangorDto | undefined,
) {
  const options = virksomheter
    .sort((a, b) => a.navn.localeCompare(b.navn))
    .map((virksomhet) => ({
      value: virksomhet.organisasjonsnummer,
      label: `${virksomhet.navn} - ${virksomhet.organisasjonsnummer}`,
    }));

  if (isArrangorMissingFromOptions(arrangor, options)) {
    options.push({
      label: `${arrangor.navn} - ${arrangor.organisasjonsnummer}`,
      value: arrangor.organisasjonsnummer,
    });
  }

  return options;
}

function isArrangorMissingFromOptions(
  arrangor: ArrangorDto | undefined,
  options: SelectOption[],
): arrangor is ArrangorDto {
  return !!arrangor && !options.some((o) => o.value === arrangor.organisasjonsnummer);
}

function getArrangorUnderenhetOptions(
  underenheter: BrregUnderenhetDto[],
  valgteOrgnr: string[],
): SelectOption[] {
  const brregOrgnr = new Set(underenheter.map((u) => u.organisasjonsnummer));

  const baseOptions: SelectOption[] = underenheter.map((virksomhet) => ({
    value: virksomhet.organisasjonsnummer,
    label: `${virksomhet.navn} - ${virksomhet.organisasjonsnummer}`,
  }));

  const missingOptions: SelectOption[] = valgteOrgnr
    .filter((orgnr) => !brregOrgnr.has(orgnr))
    .map((orgnr) => ({
      value: orgnr,
      label: orgnr,
    }));

  return [...baseOptions, ...missingOptions];
}

function getArrangorKontaktpersonOptions(kontaktpersoner: ArrangorKontaktperson[]): SelectOption[] {
  return kontaktpersoner
    .filter((person) => person.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.AVTALE))
    .map((person) => ({
      value: person.id,
      label: person.navn,
    }));
}
