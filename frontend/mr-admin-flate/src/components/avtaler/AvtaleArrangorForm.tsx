import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { useSyncArrangorFromBrreg } from "@/api/arrangor/useSyncArrangorFromBrreg";
import { Alert, UNSAFE_Combobox, VStack } from "@navikt/ds-react";
import {
  ArrangorDto,
  ArrangorKontaktperson,
  ArrangorKontaktpersonAnsvar,
  BrregHovedenhetDto,
  BrregUnderenhetDto,
} from "@tiltaksadministrasjon/api-client";
import { useRef, useState } from "react";
import { Controller, DeepPartial, useFormContext } from "react-hook-form";
import { ArrangorKontaktpersonerModal } from "@/components/arrangor/ArrangorKontaktpersonerModal";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { AvtaleFormValues } from "@/schemas/avtale";
import { FormGroup } from "@/components/skjema/FormGroup";
import { KontaktpersonButton } from "@/components/kontaktperson/KontaktpersonButton";
import { useSokBrregHovedenhet } from "@/api/virksomhet/useSokBrregHovedenhet";
import { useBrregUnderenheter } from "@/api/virksomhet/useBrregUnderenheter";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { SelectOption } from "@mr/frontend-common/components/SokeSelect";

export function AvtaleArrangorForm() {
  const arrangorKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);
  const [sokArrangor, setSokArrangor] = useState("");

  const { data: brregVirksomheter = [] } = useSokBrregHovedenhet(sokArrangor);

  const {
    watch,
    setValue,
    control,
    formState: { errors },
  } = useFormContext<DeepPartial<AvtaleFormValues>>();
  const watchedArrangor = watch("detaljer.arrangor.hovedenhet") ?? "";

  const { data: arrangor } = useSyncArrangorFromBrreg(watchedArrangor);
  const { data: underenheter } = useBrregUnderenheter(watchedArrangor);
  const { data: kontaktpersoner } = useArrangorKontaktpersoner(arrangor?.id ?? "");

  const arrangorHovedenhetOptions = getArrangorHovedenhetOptions(brregVirksomheter, arrangor);
  const arrangorUnderenhetOptions = getArrangorUnderenhetOptions(underenheter ?? []);
  const arrangorKontaktpersonOptions = getArrangorKontaktpersonOptions(kontaktpersoner ?? []);

  const underenheterIsEmpty = arrangorUnderenhetOptions.length === 0;

  return (
    <>
      <FormGroup>
        <Controller
          control={control}
          name="detaljer.arrangor.hovedenhet"
          render={({ field }) => (
            <UNSAFE_Combobox
              id="hovedenhet"
              label={avtaletekster.tiltaksarrangorHovedenhetLabel}
              placeholder="Navn eller organisasjonsnummer for tiltaksarrangør"
              selectedOptions={arrangorHovedenhetOptions.filter((v) =>
                field.value?.includes(v.value),
              )}
              size="small"
              onChange={setSokArrangor}
              name={field.name}
              error={errors.detaljer?.arrangor?.hovedenhet?.message}
              filteredOptions={arrangorHovedenhetOptions}
              options={arrangorHovedenhetOptions}
              onToggleSelected={(option, isSelected) => {
                if (isSelected) {
                  field.onChange(option);
                } else {
                  field.onChange(undefined);
                  setValue("detaljer.arrangor.underenheter", []);
                }
              }}
            />
          )}
        />
        {arrangor && underenheter && underenheterIsEmpty && (
          <Alert variant="warning">
            Bedriften {arrangor.navn} mangler underenheter i Brønnøysundregistrene og kan derfor
            ikke velges som tiltaksarrangør.
          </Alert>
        )}
        <Controller
          control={control}
          name="detaljer.arrangor.underenheter"
          render={({ field }) => (
            <UNSAFE_Combobox
              size="small"
              id="arrangorUnderenheter"
              label={
                <LabelWithHelpText
                  label={avtaletekster.tiltaksarrangorUnderenheterLabel}
                  helpTextTitle="Mer informasjon"
                >
                  Bestemmer hvilke arrangører som kan velges i gjennomføringene til avtalen.
                </LabelWithHelpText>
              }
              placeholder="Velg underenhet for tiltaksarrangør"
              isMultiSelect
              selectedOptions={arrangorUnderenhetOptions.filter((option) =>
                field.value?.includes(option.value),
              )}
              name={field.name}
              error={errors.detaljer?.arrangor?.underenheter?.message}
              options={arrangorUnderenhetOptions}
              readOnly={underenheterIsEmpty}
              onToggleSelected={(option, isSelected) => {
                const currentValues = field.value ?? [];
                if (isSelected) {
                  field.onChange([...currentValues, option]);
                } else {
                  field.onChange(currentValues.filter((v) => v !== option));
                }
              }}
            />
          )}
        />
        <VStack>
          <Controller
            control={control}
            name="detaljer.arrangor.kontaktpersoner"
            render={({ field }) => (
              <UNSAFE_Combobox
                id="arrangorKontaktpersoner"
                label={avtaletekster.kontaktpersonerHosTiltaksarrangorLabel}
                placeholder="Velg kontaktpersoner"
                isMultiSelect
                size="small"
                selectedOptions={arrangorKontaktpersonOptions.filter((v) =>
                  field.value?.includes(v.value),
                )}
                name={field.name}
                error={errors.detaljer?.arrangor?.kontaktpersoner?.message}
                options={arrangorKontaktpersonOptions}
                readOnly={!arrangor}
                onToggleSelected={(option, isSelected) => {
                  const currentValues = field.value ?? [];
                  if (isSelected) {
                    field.onChange([...currentValues, option]);
                  } else {
                    field.onChange(currentValues.filter((v) => v !== option));
                  }
                }}
              />
            )}
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

  if (arrangor) {
    options.push({
      label: `${arrangor.navn} - ${arrangor.organisasjonsnummer}`,
      value: arrangor.organisasjonsnummer,
    });
  }

  return options;
}

function getArrangorUnderenhetOptions(underenheter: BrregUnderenhetDto[]): SelectOption[] {
  return underenheter.map((virksomet) => ({
    value: virksomet.organisasjonsnummer,
    label: `${virksomet.navn} - ${virksomet.organisasjonsnummer}`,
  }));
}

function getArrangorKontaktpersonOptions(kontaktpersoner: ArrangorKontaktperson[]): SelectOption[] {
  return kontaktpersoner
    .filter((person) => person.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.AVTALE))
    .map((person) => ({
      value: person.id,
      label: person.navn,
    }));
}
