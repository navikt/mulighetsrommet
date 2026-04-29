import { useArrangorKontaktpersoner } from "@/api/arrangor/useArrangorKontaktpersoner";
import { TextField, VStack } from "@navikt/ds-react";
import {
  ArrangorKontaktperson,
  ArrangorKontaktpersonAnsvar,
  AvtaleArrangorHovedenhet,
} from "@tiltaksadministrasjon/api-client";
import { useRef } from "react";
import { useFormContext } from "react-hook-form";
import { ArrangorKontaktpersonerModal } from "../arrangor/ArrangorKontaktpersonerModal";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { KontaktpersonButton } from "@/components/kontaktperson/KontaktpersonButton";
import { GjennomforingFormInput } from "@/pages/gjennomforing/form/validation";
import { FormCombobox } from "@/components/skjema/FormCombobox";

interface Props {
  arrangor: AvtaleArrangorHovedenhet;
  readOnly: boolean;
}

export function GjennomforingArrangorForm({ readOnly, arrangor }: Props) {
  const arrangorKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const { watch, setValue } = useFormContext<GjennomforingFormInput>();

  const { data: arrangorKontaktpersoner } = useArrangorKontaktpersoner(arrangor.id);

  const arrangorOptions = getArrangorOptions(arrangor);
  const kontaktpersonOptions = getKontaktpersonOptions(arrangorKontaktpersoner ?? []);
  return (
    <>
      <VStack gap="space-16">
        <TextField
          size="small"
          label={gjennomforingTekster.tiltaksarrangorHovedenhetLabel}
          defaultValue={`${arrangor.navn} - ${arrangor.organisasjonsnummer}`}
          readOnly
        />
        <FormCombobox
          label={gjennomforingTekster.tiltaksarrangorUnderenhetLabel}
          name={"arrangorId"}
          readOnly={readOnly}
          options={arrangorOptions}
        />
        <VStack>
          <FormCombobox
            isMultiSelect
            label={gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
            name={"arrangorKontaktpersoner"}
            readOnly={!arrangor}
            options={kontaktpersonOptions}
            placeholder="Velg kontaktpersoner"
          />
          <KontaktpersonButton
            onClick={() => arrangorKontaktpersonerModalRef.current?.showModal()}
            knappetekst="Opprett eller rediger kontaktpersoner"
          />
        </VStack>
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
