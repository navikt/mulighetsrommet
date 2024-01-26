import { Alert } from "@navikt/ds-react";
import usePreviewTiltaksgjennomforingById from "../../core/api/queries/usePreviewTiltaksgjennomforingById";
import ViewTiltaksgjennomforingDetaljer from "../ViewTiltaksgjennomforingDetaljer/ViewTiltaksgjennomforingDetaljer";
import Tilbakeknapp from "../../components/tilbakeknapp/Tilbakeknapp";
import { FilterLoader } from "../../components/FilterLoader";

export function NavArbeidsmarkedstiltakViewTiltaksgjennomforingDetaljer() {
  const { data, isLoading, isError } = usePreviewTiltaksgjennomforingById();

  if (isLoading) {
    return <FilterLoader />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!data) return <Alert variant="error">Klarte ikke finne tiltaksgjennomføringen</Alert>;

  return (
    <ViewTiltaksgjennomforingDetaljer
      tiltaksgjennomforing={data}
      knapperad={<Tilbakeknapp tilbakelenke="/nav" tekst="Tilbake til tiltaksoversikten" />}
      brukerActions={null}
    />
  );
}
