import { Button } from "@navikt/ds-react";
import {
  BrregVirksomhet,
  LagretVirksomhet,
  VirksomhetKontaktperson,
} from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common/components/ControlledSokeSelect";
import { useRef, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { useSokVirksomheter } from "../../api/virksomhet/useSokVirksomhet";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { useVirksomhetKontaktpersoner } from "../../api/virksomhet/useVirksomhetKontaktpersoner";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../skjema/FormGroup";
import skjemastyles from "../skjema/Skjema.module.scss";
import { VirksomhetKontaktpersonerModal } from "../virksomhet/VirksomhetKontaktpersonerModal";
import { InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";
import { SelectOption } from "../../../../frontend-common/components/SokeSelect";

interface Props {
  readOnly: boolean;
}

export function AvtaleArrangorSkjema({ readOnly }: Props) {
  const virksomhetKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const [sokLeverandor, setSokLeverandor] = useState("");
  const { data: leverandorVirksomheter = [] } = useSokVirksomheter(sokLeverandor);

  const { register, watch, setValue } = useFormContext<DeepPartial<InferredAvtaleSchema>>();
  const watchedLeverandor = watch("leverandor") ?? "";

  const { data: leverandor } = useVirksomhet(watchedLeverandor);
  const { data: virksomhetKontaktpersoner } = useVirksomhetKontaktpersoner(leverandor?.id);

  const leverandorOptions = getLeverandorOptions(leverandorVirksomheter, leverandor);
  const underenheterOptions = getUnderenheterOptions(leverandor?.underenheter ?? []);
  const kontaktpersonOptions = getKontaktpersonOptions(virksomhetKontaktpersoner ?? []);

  return (
    <>
      <FormGroup>
        <ControlledSokeSelect
          size="small"
          readOnly={readOnly}
          placeholder="Skriv for å søke etter tiltaksarrangør"
          label={"Tiltaksarrangør hovedenhet"}
          {...register("leverandor")}
          onInputChange={(value) => {
            setSokLeverandor(value);
          }}
          onChange={(e) => {
            if (e.target.value !== watchedLeverandor) {
              setValue("leverandorUnderenheter", []);
            }
          }}
          onClearValue={() => {
            setValue("leverandor", "");
            setValue("leverandorUnderenheter", []);
          }}
          options={leverandorOptions}
        />
        <ControlledMultiSelect
          size="small"
          placeholder="Velg underenhet for tiltaksarrangør"
          label={"Tiltaksarrangør underenhet"}
          helpText="Bestemmer hvilke arrangører som kan velges i gjennomføringene til avtalen."
          readOnly={!leverandor}
          {...register("leverandorUnderenheter")}
          options={underenheterOptions}
        />
      </FormGroup>
      <FormGroup>
        <div className={skjemastyles.virksomhet_kontaktperson_container}>
          <ControlledSokeSelect
            size="small"
            placeholder="Velg en"
            label={"Kontaktperson hos leverandør"}
            readOnly={!leverandor}
            {...register("leverandorKontaktpersonId")}
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
      </FormGroup>
      {leverandor && (
        <VirksomhetKontaktpersonerModal
          virksomhetId={leverandor.id}
          modalRef={virksomhetKontaktpersonerModalRef}
        />
      )}
    </>
  );
}

function getLeverandorOptions(
  virksomheter: BrregVirksomhet[],
  leverandor: LagretVirksomhet | undefined,
) {
  const options = virksomheter
    .sort((a, b) => a.navn.localeCompare(b.navn))
    .map((enhet) => ({
      value: enhet.organisasjonsnummer,
      label: `${enhet.navn} - ${enhet.organisasjonsnummer}`,
    }));

  if (leverandor) {
    options.push({
      label: `${leverandor.navn} - ${leverandor.organisasjonsnummer}`,
      value: leverandor.organisasjonsnummer,
    });
  }

  return options;
}

function getUnderenheterOptions(underenheterForLeverandor: BrregVirksomhet[]): SelectOption[] {
  return underenheterForLeverandor.map((leverandor) => ({
    value: leverandor.organisasjonsnummer,
    label: `${leverandor.navn} - ${leverandor.organisasjonsnummer}`,
  }));
}

function getKontaktpersonOptions(virksomhetKontaktpersoner: VirksomhetKontaktperson[]) {
  return virksomhetKontaktpersoner.map((person) => ({
    value: person.id,
    label: person.navn,
  }));
}
