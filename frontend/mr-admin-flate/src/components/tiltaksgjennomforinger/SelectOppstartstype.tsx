import { Alert } from "@navikt/ds-react";
import { TiltaksgjennomforingOppstartstype } from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";
import { useController } from "react-hook-form";
import { useTiltaksgjennomforingDeltakerSummary } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingDeltakerSummary";
import { Laster } from "../laster/Laster";
import { useGetTiltaksgjennomforingIdFromUrl } from "../../hooks/useGetTiltaksgjennomforingIdFromUrl";

interface SelectOppstartstypeProps {
  name: string;
}

export function SelectOppstartstype({ name }: SelectOppstartstypeProps) {
  const id = useGetTiltaksgjennomforingIdFromUrl();

  const { field, fieldState } = useController({ name });

  const valueHasChanged = id !== undefined && fieldState.isDirty;

  return (
    <>
      <ControlledSokeSelect
        size="small"
        label="Oppstartstype"
        placeholder="Velg oppstart"
        name={name}
        onChange={field.onChange}
        options={[
          {
            label: "Felles oppstartsdato",
            value: TiltaksgjennomforingOppstartstype.FELLES,
          },
          {
            label: "Løpende oppstart",
            value: TiltaksgjennomforingOppstartstype.LOPENDE,
          },
        ]}
      />
      {valueHasChanged && <OppstartstypeWarning gjennomforingId={id} />}
    </>
  );
}

interface OppstartstypePropsWarning {
  gjennomforingId: string;
}

function OppstartstypeWarning({ gjennomforingId }: OppstartstypePropsWarning) {
  const {
    data: summary,
    isPending,
    isError,
  } = useTiltaksgjennomforingDeltakerSummary(gjennomforingId);

  if (isPending) {
    return <Laster />;
  }

  return isError || summary.antallDeltakere > 0 ? (
    <Alert variant="warning">
      Deltakerstatus påvirkes av oppstartstypen. Hvis du endrer oppstartstypen så kan deltakelser
      som er avsluttet få en ny status. Statusen vises i aktivitetsplanen og deltakeroversikten.
    </Alert>
  ) : null;
}
