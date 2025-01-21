import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { Button, Textarea, TextField, VStack } from "@navikt/ds-react";
import { ArrangorKontaktperson, ArrangorKontaktpersonAnsvar, AvtaleDto } from "@mr/api-client-v2";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { useRef } from "react";
import { useFormContext } from "react-hook-form";
import { ArrangorKontaktpersonerModal } from "../arrangor/ArrangorKontaktpersonerModal";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { InferredGjennomforingSchema } from "@/components/redaksjoneltInnhold/GjennomforingSchema";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { STED_FOR_GJENNOMFORING_MAX_LENGTH } from "@/constants";
import { KontaktpersonButton } from "@/components/kontaktperson/KontaktpersonButton";

interface Props {
  avtale: AvtaleDto;
  readOnly: boolean;
}

export function GjennomforingArrangorForm({ readOnly, avtale }: Props) {
  const arrangorKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const {
    register,
    watch,
    formState: { errors },
    setValue,
  } = useFormContext<InferredGjennomforingSchema>();

  const { data: arrangorKontaktpersoner } = useArrangorKontaktpersoner(avtale.arrangor.id);

  const arrangorOptions = getArrangorOptions(avtale);
  const kontaktpersonOptions = getKontaktpersonOptions(arrangorKontaktpersoner ?? []);
  return (
    <>
      <VStack gap="2">
        <TextField
          size="small"
          label={gjennomforingTekster.tiltaksarrangorHovedenhetLabel}
          placeholder=""
          defaultValue={`${avtale.arrangor.navn} - ${avtale.arrangor.organisasjonsnummer}`}
          readOnly
        />
        <ControlledSokeSelect
          size="small"
          label={gjennomforingTekster.tiltaksarrangorUnderenhetLabel}
          placeholder="Velg underenhet for tiltaksarrangør"
          {...register("arrangorId")}
          onClearValue={() => {
            setValue("arrangorId", "");
          }}
          readOnly={readOnly}
          options={arrangorOptions}
        />
        <VStack>
          <ControlledMultiSelect
            size="small"
            placeholder="Velg kontaktpersoner"
            label={gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
            {...register("arrangorKontaktpersoner")}
            options={kontaktpersonOptions}
            noOptionsMessage={
              <Button
                size="small"
                type="button"
                variant="tertiary"
                onClick={() => arrangorKontaktpersonerModalRef.current?.showModal()}
              >
                Opprett kontaktpersoner
              </Button>
            }
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
        arrangorId={avtale.arrangor.id}
        modalRef={arrangorKontaktpersonerModalRef}
        onOpprettSuccess={(kontaktperson) => {
          if (!kontaktperson.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.GJENNOMFORING)) {
            return;
          }

          const kontaktpersoner = watch("arrangorKontaktpersoner") ?? [];
          setValue("arrangorKontaktpersoner", [
            ...kontaktpersoner.filter((k) => k !== kontaktperson.id),
            kontaktperson.id,
          ]);
        }}
      />
    </>
  );
}

function getArrangorOptions(avtale: AvtaleDto) {
  return avtale.arrangor.underenheter
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
