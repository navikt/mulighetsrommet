import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { Button, Textarea, TextField, VStack } from "@navikt/ds-react";
import { ArrangorKontaktperson, ArrangorKontaktpersonAnsvar, Avtale } from "@mr/api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";
import { useRef } from "react";
import { useFormContext } from "react-hook-form";
import { ArrangorKontaktpersonerModal } from "../arrangor/ArrangorKontaktpersonerModal";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";
import { InferredTiltaksgjennomforingSchema } from "@/components/redaksjoneltInnhold/TiltaksgjennomforingSchema";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { STED_FOR_GJENNOMFORING_MAX_LENGTH } from "@/constants";
import { ArrangorKontaktpersonContainer } from "@/components/skjema/ArrangorKontaktpersonContainer";
import { KontaktpersonButton } from "@/components/kontaktperson/KontaktpersonButton";

interface Props {
  avtale: Avtale;
  readOnly: boolean;
}

export function TiltaksgjennomforingArrangorSkjema({ readOnly, avtale }: Props) {
  const arrangorKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const {
    register,
    watch,
    formState: { errors },
    setValue,
  } = useFormContext<InferredTiltaksgjennomforingSchema>();

  const { data: arrangorKontaktpersoner } = useArrangorKontaktpersoner(avtale.arrangor.id);

  const arrangorOptions = getArrangorOptions(avtale);
  const kontaktpersonOptions = getKontaktpersonOptions(arrangorKontaktpersoner ?? []);
  return (
    <>
      <VStack gap="2">
        <TextField
          size="small"
          label={tiltaktekster.tiltaksarrangorHovedenhetLabel}
          placeholder=""
          defaultValue={`${avtale.arrangor.navn} - ${avtale.arrangor.organisasjonsnummer}`}
          readOnly
        />
        <ControlledSokeSelect
          size="small"
          label={tiltaktekster.tiltaksarrangorUnderenhetLabel}
          placeholder="Velg underenhet for tiltaksarrangør"
          {...register("arrangorId")}
          onClearValue={() => {
            setValue("arrangorId", "");
          }}
          readOnly={readOnly}
          options={arrangorOptions}
        />
        <ArrangorKontaktpersonContainer>
          <ControlledMultiSelect
            size="small"
            placeholder="Velg kontaktpersoner"
            label={tiltaktekster.kontaktpersonerHosTiltaksarrangorLabel}
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
        </ArrangorKontaktpersonContainer>
        <Textarea
          size="small"
          resize
          value={watch("stedForGjennomforing") || ""}
          maxLength={STED_FOR_GJENNOMFORING_MAX_LENGTH}
          label={tiltaktekster.stedForGjennomforingLabel}
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
          if (
            !kontaktperson.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.TILTAKSGJENNOMFORING)
          ) {
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

function getArrangorOptions(avtale: Avtale) {
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
    .filter((person) =>
      person.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.TILTAKSGJENNOMFORING),
    )
    .map((person) => ({
      value: person.id,
      label: person.navn,
    }));
}
