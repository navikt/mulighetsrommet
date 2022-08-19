import React, { useState } from "react";
import "./ViewTiltaksgjennomforingDetaljer.less";
import Tilbakeknapp from "../../components/tilbakeknapp/Tilbakeknapp";
import TiltaksgjennomforingsHeader from "../../layouts/TiltaksgjennomforingsHeader";
import Nokkelinfo from "../../components/nokkelinfo/Nokkelinfo";
import SidemenyDetaljer from "../../components/sidemeny/SidemenyDetaljer";
import TiltaksdetaljerFane from "../../components/tabs/TiltaksdetaljerFane";
import useTiltaksgjennomforingByTiltaksnummer from "../../core/api/queries/useTiltaksgjennomforingByTiltaksnummer";
import { Alert, Loader } from "@navikt/ds-react";
import { useGetTiltaksnummerFraUrl } from "../../core/api/queries/useGetTiltaksnummerFraUrl";
import { useHentFnrFraUrl } from "../../hooks/useHentFnrFraUrl";
import Deleknapp from "../../components/knapper/Deleknapp";
import Delemodal from "../../components/modal/Delemodal";

const ViewTiltakstypeDetaljer = () => {
  const tiltaksnummer = useGetTiltaksnummerFraUrl();
  const fnr = useHentFnrFraUrl();
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingByTiltaksnummer();
  const [delemodalApen, setDelemodalApen] = useState<boolean>(false);

  const handleClickApneModal = () => {
    setDelemodalApen(true);
  };

  if (isLoading) {
    return <Loader className="filter-loader" size="xlarge" />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!tiltaksgjennomforing) {
    return (
      <Alert variant="warning">{`Det finnes ingen tiltaksgjennomføringer med tiltaksnummer "${tiltaksnummer}"`}</Alert>
    );
  }
  return (
    <div className="tiltakstype-detaljer">
      <div className="tiltakstype-detaljer__info">
        <Tilbakeknapp tilbakelenke={`/${fnr}`} />
        <TiltaksgjennomforingsHeader />
        {tiltaksgjennomforing.tiltakstype.nokkelinfoKomponenter && (
          <Nokkelinfo nokkelinfoKomponenter={tiltaksgjennomforing.tiltakstype.nokkelinfoKomponenter} />
        )}
      </div>
      <div>
        <SidemenyDetaljer />
        <Deleknapp ariaLabel={"Dele"} handleClick={handleClickApneModal}>Del med bruker </Deleknapp>
      </div>
      <TiltaksdetaljerFane />
      <Delemodal modalOpen={delemodalApen} setModalOpen={() => setDelemodalApen(false)}
                 tiltaksgjennomforingsnavn={tiltaksgjennomforing.tiltaksgjennomforingNavn} brukerNavn={"NAVN"}
                 chattekst={tiltaksgjennomforing.tiltakstype.chattekst ?? ""} />
    </div>
  );
};

export default ViewTiltakstypeDetaljer;
