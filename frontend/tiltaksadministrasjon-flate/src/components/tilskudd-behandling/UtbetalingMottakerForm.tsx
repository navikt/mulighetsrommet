import {
  BrukerUtbetalingType,
  TilskuddBehandlingRequest,
  TilskuddMottakerType,
  TilskuddVedtakRequest,
} from "@tiltaksadministrasjon/api-client";
import { Path, useFormContext } from "react-hook-form";
import { ControlledRadioGroup } from "../skjema/ControlledRadioGroup";
import { Bleed, BodyShort, Radio } from "@navikt/ds-react";
import { BetalingsinformasjonFields } from "../utbetaling/form/BetalingsinformasjonFields";

interface UtbetalingMottakerFormProps {
  index: number;
  arrangorId: string;
}

export function UtbetalingMottakerForm({ index, arrangorId }: UtbetalingMottakerFormProps) {
  const { watch } = useFormContext<TilskuddBehandlingRequest>();
  const tilskuddVedtak = watch("tilskuddVedtak")[index];

  return (
    <>
      <ControlledRadioGroup
        size="small"
        name={`tilskuddVedtak.${index}.utbetalingMottaker`}
        legend="Hvem skal motta utbetalingen?"
        horisontal
      >
        <Radio value={TilskuddMottakerType.BRUKER}>Utbetales til brukeren</Radio>
        <Radio value={TilskuddMottakerType.ARRANGOR}>Utbetales til arrangøren</Radio>
      </ControlledRadioGroup>
      {tilskuddVedtak.utbetalingMottaker === TilskuddMottakerType.BRUKER && (
        <>
          <ControlledRadioGroup
            size="small"
            name={`tilskuddVedtak.${index}.brukerUtbetalingType`}
            legend="Har bruker lagt ut for tilskuddet selv?"
            horisontal
          >
            <Radio value={BrukerUtbetalingType.REFUSJON}>
              Ja, bruker har betalt til utdanningsinstitusjonen
            </Radio>
            <Radio value={BrukerUtbetalingType.FORSKUDD}>
              Nei, det skal utbetales som forskudd
            </Radio>
          </ControlledRadioGroup>
          <Bleed marginBlock="space-16 space-0">
            <BodyShort size="small">
              Husk at det skal være avklart med Nav-veileder at bruker skal få utbetalt som forskudd
            </BodyShort>
          </Bleed>
        </>
      )}
      {tilskuddVedtak.utbetalingMottaker === TilskuddMottakerType.ARRANGOR && (
        <>
          <BetalingsinformasjonFields<TilskuddVedtakRequest>
            arrangorId={arrangorId}
            referanseName={
              `tilskuddVedtak.${index}.arrangorReferanse` as Path<TilskuddVedtakRequest>
            }
            kidNummerName={`tilskuddVedtak.${index}.kidNummer` as Path<TilskuddVedtakRequest>}
            fakturanummerName={
              `tilskuddVedtak.${index}.fakturanummer` as Path<TilskuddVedtakRequest>
            }
          />
        </>
      )}
    </>
  );
}
