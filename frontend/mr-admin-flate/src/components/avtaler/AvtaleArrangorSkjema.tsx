import { Button } from "@navikt/ds-react";
import { BrregVirksomhet, Virksomhet, VirksomhetKontaktperson } from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common/components/ControlledSokeSelect";
import { useRef, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { useSokVirksomheter } from "../../api/virksomhet/useSokVirksomhet";
import {
  useBrregVirksomhetUnderenheter,
  useSyncBrregVirksomhet,
} from "../../api/virksomhet/useBrregVirksomhet";
import { useVirksomhetKontaktpersoner } from "../../api/virksomhet/useVirksomhetKontaktpersoner";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../skjema/FormGroup";
import skjemastyles from "../skjema/Skjema.module.scss";
import { VirksomhetKontaktpersonerModal } from "../virksomhet/VirksomhetKontaktpersonerModal";
import { InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";
import { SelectOption } from "../../../../frontend-common/components/SokeSelect";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";

interface Props {
  readOnly: boolean;
}

export function AvtaleArrangorSkjema({ readOnly }: Props) {
  const virksomhetKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const [sokLeverandor, setSokLeverandor] = useState("");
  const { data: leverandorVirksomheter = [] } = useSokVirksomheter(sokLeverandor);

  const { register, watch, setValue } = useFormContext<DeepPartial<InferredAvtaleSchema>>();
  const watchedLeverandor = watch("leverandor") ?? "";

  const { data: leverandor } = useSyncBrregVirksomhet(watchedLeverandor);
  const { data: leverandorUnderenheter } = useBrregVirksomhetUnderenheter(watchedLeverandor);
  const { data: virksomhetKontaktpersoner } = useVirksomhetKontaktpersoner(leverandor?.id);

  const leverandorOptions = getLeverandorOptions(leverandorVirksomheter, leverandor);
  const underenheterOptions = getUnderenheterOptions(leverandorUnderenheter ?? []);
  const kontaktpersonOptions = getKontaktpersonOptions(virksomhetKontaktpersoner ?? []);

  return (
    <>
      <FormGroup>
        <ControlledSokeSelect
          size="small"
          readOnly={readOnly}
          placeholder="Søk på navn eller organisasjonsnummer for tiltaksarrangør"
          label={avtaletekster.tiltaksarrangorHovedenhetLabel}
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
          label={avtaletekster.tiltaksarrangorUnderenheterLabel}
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
            label={avtaletekster.kontaktpersonHosTiltaksarrangorLabel}
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

function getLeverandorOptions(virksomheter: BrregVirksomhet[], leverandor: Virksomhet | undefined) {
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
