import { Button, TextField } from "@navikt/ds-react";
import { Avtale, VirksomhetKontaktperson } from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";
import { useRef } from "react";
import { useFormContext } from "react-hook-form";
import { useVirksomhetKontaktpersoner } from "../../api/virksomhet/useVirksomhetKontaktpersoner";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../skjema/FormGroup";
import skjemastyles from "../skjema/Skjema.module.scss";
import { VirksomhetKontaktpersonerModal } from "../virksomhet/VirksomhetKontaktpersonerModal";
import { InferredTiltaksgjennomforingSchema } from "../redaksjonelt-innhold/TiltaksgjennomforingSchema";

interface Props {
  avtale: Avtale;
  readOnly: boolean;
}

export function TiltaksgjennomforingArrangorSkjema({ readOnly, avtale }: Props) {
  const virksomhetKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const {
    register,
    formState: { errors },
    setValue,
  } = useFormContext<InferredTiltaksgjennomforingSchema>();

  const { data: virksomhetKontaktpersoner } = useVirksomhetKontaktpersoner(avtale.leverandor.id);

  const arrangorOptions = getArrangorOptions(avtale);
  const kontaktpersonOptions = getKontaktpersonOptions(virksomhetKontaktpersoner ?? []);

  return (
    <>
      <FormGroup>
        <TextField
          size="small"
          label="Tiltaksarrangør hovedenhet"
          placeholder=""
          defaultValue={`${avtale.leverandor.navn} - ${avtale.leverandor.organisasjonsnummer}`}
          readOnly
        />
        <ControlledSokeSelect
          size="small"
          label="Tiltaksarrangør underenhet"
          placeholder="Velg underenhet for tiltaksarrangør"
          {...register("arrangorVirksomhetId")}
          onClearValue={() => {
            setValue("arrangorVirksomhetId", "");
          }}
          readOnly={readOnly}
          options={arrangorOptions}
        />
        <div className={skjemastyles.virksomhet_kontaktperson_container}>
          <ControlledMultiSelect
            size="small"
            placeholder="Velg kontaktpersoner"
            label={"Kontaktpersoner hos tiltaksarrangør"}
            {...register("arrangorKontaktpersoner")}
            options={kontaktpersonOptions}
          />
          <Button
            className={skjemastyles.kontaktperson_button}
            size="small"
            type="button"
            variant="tertiary"
            onClick={() => virksomhetKontaktpersonerModalRef.current?.showModal()}
          >
            Rediger eller legg til kontaktpersoner
          </Button>
        </div>
        <TextField
          size="small"
          label="Sted for gjennomføring"
          description="Skriv inn stedet tiltaket skal gjennomføres, for eksempel Fredrikstad eller Tromsø. For tiltak uten eksplisitt lokasjon (for eksempel digital jobbklubb), kan du la feltet stå tomt."
          {...register("stedForGjennomforing")}
          error={
            errors.stedForGjennomforing ? (errors.stedForGjennomforing.message as string) : null
          }
        />
      </FormGroup>
      <VirksomhetKontaktpersonerModal
        virksomhetId={avtale.leverandor.id}
        modalRef={virksomhetKontaktpersonerModalRef}
      />
    </>
  );
}

function getArrangorOptions(avtale: Avtale) {
  return avtale.leverandor.underenheter
    .sort((a, b) => a.navn.localeCompare(b.navn))
    .map((arrangor) => {
      return {
        label: `${arrangor.navn} - ${arrangor.organisasjonsnummer}`,
        value: arrangor.id,
      };
    });
}

function getKontaktpersonOptions(kontaktpersoner: VirksomhetKontaktperson[]) {
  return kontaktpersoner.map((person) => ({
    value: person.id,
    label: person.navn,
  }));
}
