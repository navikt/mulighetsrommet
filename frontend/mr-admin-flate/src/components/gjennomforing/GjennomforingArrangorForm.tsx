import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { Textarea, TextField, UNSAFE_Combobox, VStack } from "@navikt/ds-react";
import {
  ArrangorKontaktperson,
  ArrangorKontaktpersonAnsvar,
} from "@tiltaksadministrasjon/api-client";
import { useRef } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { ArrangorKontaktpersonerModal } from "../arrangor/ArrangorKontaktpersonerModal";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { InferredGjennomforingSchema } from "@/components/redaksjoneltInnhold/GjennomforingSchema";
import { STED_FOR_GJENNOMFORING_MAX_LENGTH } from "@/constants";
import { KontaktpersonButton } from "@/components/kontaktperson/KontaktpersonButton";
import { AvtaleArrangorHovedenhet } from "@mr/api-client-v2";

interface Props {
  arrangor: AvtaleArrangorHovedenhet;
  readOnly: boolean;
}

export function GjennomforingArrangorForm({ readOnly, arrangor }: Props) {
  const arrangorKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const {
    register,
    watch,
    formState: { errors },
    setValue,
    control,
  } = useFormContext<InferredGjennomforingSchema>();

  const { data: arrangorKontaktpersoner } = useArrangorKontaktpersoner(arrangor.id);

  const arrangorOptions = getArrangorOptions(arrangor);
  const kontaktpersonOptions = getKontaktpersonOptions(arrangorKontaktpersoner ?? []);
  return (
    <>
      <VStack gap="4">
        <TextField
          size="small"
          label={gjennomforingTekster.tiltaksarrangorHovedenhetLabel}
          defaultValue={`${arrangor.navn} - ${arrangor.organisasjonsnummer}`}
          readOnly
        />
        <Controller
          control={control}
          name="arrangorId"
          render={({ field }) => (
            <UNSAFE_Combobox
              size="small"
              id="arrangorId"
              label={gjennomforingTekster.tiltaksarrangorUnderenhetLabel}
              placeholder="Velg underenhet for tiltaksarrangør"
              selectedOptions={arrangorOptions.filter((option) =>
                field.value?.includes(option.value),
              )}
              name={field.name}
              error={errors.arrangorId?.message}
              options={arrangorOptions}
              readOnly={readOnly}
              onToggleSelected={(option, isSelected) => {
                if (isSelected) {
                  field.onChange(option);
                } else {
                  field.onChange(undefined);
                }
              }}
            />
          )}
        />
        <VStack>
          <Controller
            control={control}
            name="arrangorKontaktpersoner"
            render={({ field }) => (
              <UNSAFE_Combobox
                size="small"
                id="arrangorKontaktpersoner"
                label={gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
                placeholder="Velg kontaktpersoner"
                isMultiSelect
                selectedOptions={kontaktpersonOptions.filter((v) => field.value?.includes(v.value))}
                name={field.name}
                error={errors.arrangorKontaktpersoner?.message}
                options={kontaktpersonOptions}
                readOnly={!arrangor}
                onToggleSelected={(option, isSelected) => {
                  const currentValues = field.value;
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
        <Textarea
          size="small"
          resize
          value={watch("stedForGjennomforing") || ""}
          maxLength={STED_FOR_GJENNOMFORING_MAX_LENGTH}
          label={gjennomforingTekster.stedForGjennomforingLabel}
          description="Skriv inn stedet tiltaket skal gjennomføres, for eksempel Fredrikstad eller Tromsø. For tiltak uten eksplisitt lokasjon (for eksempel digital jobbklubb), kan du la feltet stå tomt."
          {...register("stedForGjennomforing")}
          error={
            errors.stedForGjennomforing ? (errors.stedForGjennomforing.message as string) : null
          }
        />
      </VStack>

      <ArrangorKontaktpersonerModal
        arrangorId={arrangor.id}
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
    </>
  );
}

function getArrangorOptions(arrangor: AvtaleArrangorHovedenhet) {
  return arrangor.underenheter
    .sort((a, b) => a.navn.localeCompare(b.navn))
    .map((arrangor) => {
      return {
        label: `${arrangor.navn} - ${arrangor.organisasjonsnummer}`,
        value: arrangor.id,
      };
    });
}

function getKontaktpersonOptions(kontaktpersoner: ArrangorKontaktperson[]) {
  return kontaktpersoner
    .filter((person) => person.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.GJENNOMFORING))
    .map((person) => ({
      value: person.id,
      label: person.navn,
    }));
}
