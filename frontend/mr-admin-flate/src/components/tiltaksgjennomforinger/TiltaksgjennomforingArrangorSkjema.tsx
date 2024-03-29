import { Button, TextField } from "@navikt/ds-react";
import { ArrangorKontaktperson, Avtale } from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";
import { useRef } from "react";
import { useFormContext } from "react-hook-form";
import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../skjema/FormGroup";
import skjemastyles from "../skjema/Skjema.module.scss";
import { ArrangorKontaktpersonerModal } from "../arrangor/ArrangorKontaktpersonerModal";
import { InferredTiltaksgjennomforingSchema } from "../redaksjonelt-innhold/TiltaksgjennomforingSchema";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";

interface Props {
  avtale: Avtale;
  readOnly: boolean;
}

export function TiltaksgjennomforingArrangorSkjema({ readOnly, avtale }: Props) {
  const arrangorKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const {
    register,
    formState: { errors },
    setValue,
  } = useFormContext<InferredTiltaksgjennomforingSchema>();

  const { data: arrangorKontaktpersoner } = useArrangorKontaktpersoner(avtale.arrangor.id);

  const arrangorOptions = getArrangorOptions(avtale);
  const kontaktpersonOptions = getKontaktpersonOptions(arrangorKontaktpersoner ?? []);

  return (
    <>
      <FormGroup>
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
        <div className={skjemastyles.arrangor_kontaktperson_container}>
          <ControlledMultiSelect
            size="small"
            placeholder="Velg kontaktpersoner"
            label={tiltaktekster.kontaktpersonerHosTiltaksarrangorLabel}
            {...register("arrangorKontaktpersoner")}
            options={kontaktpersonOptions}
          />
          <Button
            className={skjemastyles.kontaktperson_button}
            size="small"
            type="button"
            variant="tertiary"
            onClick={() => arrangorKontaktpersonerModalRef.current?.showModal()}
          >
            Opprett eller rediger kontaktpersoner
          </Button>
        </div>
        <TextField
          size="small"
          label={tiltaktekster.stedForGjennomforingLabel}
          description="Skriv inn stedet tiltaket skal gjennomføres, for eksempel Fredrikstad eller Tromsø. For tiltak uten eksplisitt lokasjon (for eksempel digital jobbklubb), kan du la feltet stå tomt."
          {...register("stedForGjennomforing")}
          error={
            errors.stedForGjennomforing ? (errors.stedForGjennomforing.message as string) : null
          }
        />
      </FormGroup>
      <ArrangorKontaktpersonerModal
        arrangorId={avtale.arrangor.id}
        modalRef={arrangorKontaktpersonerModalRef}
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
  return kontaktpersoner.map((person) => ({
    value: person.id,
    label: person.navn,
  }));
}
