import { useFormContext } from "react-hook-form";
import { TiltakBeskrivelseFormValues } from "@/pages/tiltak-beskrivelse/TiltakBeskrivelseFormValues";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { FormComboboxMulti } from "@/components/skjema/FormComboboxMulti";
import { useNavAnsatte } from "@/api/ansatt/useNavAnsatte";
import {
  Rolle,
  SortDirection,
  TiltakstypeEgenskap,
  TiltakstypeSortField,
} from "@tiltaksadministrasjon/api-client";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { FormSelect } from "../skjema/FormSelect";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { VStack } from "@navikt/ds-react";

export function TiltakBeskrivelseDetaljerForm() {
  useFormContext<TiltakBeskrivelseFormValues>();

  const tiltakstyper = useTiltakstyper({
    sort: { field: TiltakstypeSortField.NAVN, direction: SortDirection.ASC },
    egenskaper: [TiltakstypeEgenskap.STOTTER_TILTAK_BESKRIVELSE],
  });
  const tiltakstypeOptions = tiltakstyper.map((t) => ({ value: t.id, label: t.navn }));

  const { data: administratorer } = useNavAnsatte([Rolle.TILTAKSGJENNOMFORINGER_SKRIV]);
  const administratorOptions = administratorer.map((a) => ({
    value: a.navIdent,
    label: `${a.fornavn} ${a.etternavn} - ${a.navIdent}`,
  }));

  return (
    <VStack gap="space-16">
      <FormTextField<TiltakBeskrivelseFormValues> name="navn" label="Navn" required />

      <FormTextField<TiltakBeskrivelseFormValues>
        name="tiltaksnummer"
        label="Tiltaksnummer (valgfritt)"
      />

      <FormSelect<TiltakBeskrivelseFormValues> name="tiltakstypeId" label="Tiltakstype">
        <option value="">-- Velg en --</option>
        {tiltakstypeOptions.map((type) => (
          <option key={type.value} value={type.value}>
            {type.label}
          </option>
        ))}
      </FormSelect>

      <FormTextarea<TiltakBeskrivelseFormValues>
        name="stedForGjennomforing"
        label="Sted for gjennomføring (valgfritt)"
        minRows={2}
        maxRows={4}
      />

      <FormComboboxMulti<TiltakBeskrivelseFormValues>
        name="administratorer"
        label={
          <LabelWithHelpText label={gjennomforingTekster.administratorerForGjennomforingenLabel}>
            Bestemmer hvem som eier gjennomføringen.
          </LabelWithHelpText>
        }
        placeholder="Administratorer"
        options={administratorOptions}
      />
    </VStack>
  );
}
