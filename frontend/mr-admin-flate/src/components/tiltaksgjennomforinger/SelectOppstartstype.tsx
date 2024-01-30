import { Alert, HelpText } from "@navikt/ds-react";
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
      som er avsluttet få en ny status. Statusen vises i aktivitetsplanen og deltakeroversikten.{" "}
      <HelpText title="Hvilke konsekvenser får dette?">
        <ul>
          <li>
            På tiltak med <code>løpende inntak</code> vil avsluttede deltakere ha statusen &quot;Har
            sluttet&quot; i aktivitetsplanen og Deltakeroversikten. Dersom gjennomføringen blir
            endret til <code>felles oppstart</code> så vil statusen endres til enten
            &quot;Fullført&quot; eller &quot;Avbrutt&quot;. Aktivitetskortet i aktivitetsplanen til
            bruker vil flyttes til statuskolonnen &quot;Avbrutt&quot; dersom statusen på
            tiltaksdeltakelsen endres til dette.
          </li>
          <li>
            På tiltak med <code>felles oppstart</code> vil avsluttede deltakere ha status
            &quot;Fullført&quot; eller &quot;Avbrutt&quot;. Dersom gjennomføringen blir endret til{" "}
            <code>løpende inntak</code> så vil disse statusene blir endret til &quot;Har
            sluttet&quot; (og aktivitetskortet i aktivitetsplanen vil ligge i statuskolonnen
            &quot;Fullført&quot;).
          </li>
          <li>
            Dersom statusen endres vil det være synlig i aktivitetsplanen at det er en endring på
            det aktuelle aktivitetskortet (en visuell markering i form av en blå prikk som betyr at
            det er en endring siden sist du var inne på planen)
          </li>
        </ul>
      </HelpText>
    </Alert>
  ) : null;
}
