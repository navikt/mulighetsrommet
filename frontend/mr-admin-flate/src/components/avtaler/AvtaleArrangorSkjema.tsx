import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { useSyncArrangorFromBrreg } from "@/api/arrangor/useSyncArrangorFromBrreg";
import { useBrregVirksomhetUnderenheter } from "@/api/virksomhet/useBrregVirksomhetUnderenheter";
import { useSokBrregVirksomheter } from "@/api/virksomhet/useSokBrregVirksomheter";
import { Alert, Button, VStack } from "@navikt/ds-react";
import {
  Arrangor,
  ArrangorKontaktperson,
  ArrangorKontaktpersonAnsvar,
  BrregVirksomhet,
} from "@mr/api-client";
import { ControlledSokeSelect } from "@mr/frontend-common/components/ControlledSokeSelect";
import { SelectOption } from "@mr/frontend-common/components/SokeSelect";
import { useRef, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { ArrangorKontaktpersonerModal } from "../arrangor/ArrangorKontaktpersonerModal";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "@/components/skjema/FormGroup";
import { KontaktpersonButton } from "@/components/kontaktperson/KontaktpersonButton";

interface Props {
  readOnly: boolean;
}

export function AvtaleArrangorSkjema({ readOnly }: Props) {
  const arrangorKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const [sokArrangor, setSokArrangor] = useState("");
  const { data: brregVirksomheter = [] } = useSokBrregVirksomheter(sokArrangor);

  const { register, watch, setValue } = useFormContext<DeepPartial<InferredAvtaleSchema>>();
  const watchedArrangor = watch("arrangorOrganisasjonsnummer") ?? "";

  const { data: arrangor } = useSyncArrangorFromBrreg(watchedArrangor);
  const { data: underenheter } = useBrregVirksomhetUnderenheter(watchedArrangor);
  const { data: kontaktpersoner } = useArrangorKontaktpersoner(arrangor?.id);

  const arrangorHovedenhetOptions = getArrangorHovedenhetOptions(brregVirksomheter, arrangor);
  const arrangorUnderenhetOptions = getArrangorUnderenhetOptions(underenheter ?? []);
  const arrangorKontaktpersonOptions = getArrangorKontaktpersonOptions(kontaktpersoner ?? []);

  const underenheterIsEmpty = arrangorUnderenhetOptions.length === 0;

  return (
    <>
      <FormGroup>
        <ControlledSokeSelect
          size="small"
          readOnly={readOnly}
          placeholder="Navn eller organisasjonsnummer for tiltaksarrangør"
          label={avtaletekster.tiltaksarrangorHovedenhetLabel}
          {...register("arrangorOrganisasjonsnummer")}
          onInputChange={(value) => {
            setSokArrangor(value);
          }}
          onChange={(e) => {
            if (e.target.value !== watchedArrangor) {
              setValue("arrangorUnderenheter", []);
            }
          }}
          onClearValue={() => {
            setValue("arrangorOrganisasjonsnummer", "");
            setValue("arrangorUnderenheter", []);
          }}
          options={arrangorHovedenhetOptions}
        />
        {arrangor && underenheter && underenheterIsEmpty && (
          <Alert variant="warning">
            Bedriften {arrangor.navn} mangler underenheter i Brønnøysundregistrene og kan derfor
            ikke velges som tiltaksarrangør.
          </Alert>
        )}
        <ControlledMultiSelect
          size="small"
          placeholder="Velg underenhet for tiltaksarrangør"
          label={avtaletekster.tiltaksarrangorUnderenheterLabel}
          helpText="Bestemmer hvilke arrangører som kan velges i gjennomføringene til avtalen."
          readOnly={underenheterIsEmpty}
          {...register("arrangorUnderenheter")}
          velgAlle
          options={arrangorUnderenhetOptions}
        />
      </FormGroup>
      <FormGroup>
        <VStack>
          <ControlledMultiSelect
            size="small"
            placeholder="Velg kontaktpersoner"
            label={avtaletekster.kontaktpersonerHosTiltaksarrangorLabel}
            readOnly={!arrangor}
            {...register("arrangorKontaktpersoner")}
            options={arrangorKontaktpersonOptions}
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
      </FormGroup>
      {arrangor && (
        <ArrangorKontaktpersonerModal
          arrangorId={arrangor.id}
          modalRef={arrangorKontaktpersonerModalRef}
          onOpprettSuccess={(kontaktperson) => {
            if (!kontaktperson.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.AVTALE)) {
              return;
            }

            const kontaktpersoner = watch("arrangorKontaktpersoner") ?? [];
            setValue("arrangorKontaktpersoner", [
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
  virksomheter: BrregVirksomhet[],
  arrangor: Arrangor | undefined,
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

function getArrangorUnderenhetOptions(underenheter: BrregVirksomhet[]): SelectOption[] {
  return underenheter.map((virksomet) => ({
    value: virksomet.organisasjonsnummer,
    label: `${virksomet.navn} - ${virksomet.organisasjonsnummer}`,
  }));
}

function getArrangorKontaktpersonOptions(kontaktpersoner: ArrangorKontaktperson[]) {
  return kontaktpersoner
    .filter((person) => person.ansvarligFor.includes(ArrangorKontaktpersonAnsvar.AVTALE))
    .map((person) => ({
      value: person.id,
      label: person.navn,
    }));
}
