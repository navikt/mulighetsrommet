import Filtermeny from '../../components/filtrering/Filtermeny';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingsTabell';
import '../../layouts/TiltaksgjennomforingsHeader.less';
import './ViewTiltakstypeOversikt.less';
import { Filtertags } from '../../components/filtrering/Filtertags';
import { HistorikkButton } from '../../components/historikk/HistorikkButton';
import { useFeatureToggles, VIS_HISTORIKK } from '../../core/api/feature-toggles';

const ViewTiltakstypeOversikt = () => {
  const features = useFeatureToggles();
  const visHistorikkKnapp = features.isSuccess && features.data[VIS_HISTORIKK];

  return (
    <div className="tiltakstype-oversikt" id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
      <div className="filtertagsOgHistorikk">
        <Filtertags />
        {visHistorikkKnapp && <HistorikkButton />}
      </div>
      <Filtermeny />
      <div className="tiltakstype-oversikt__tiltak">
        <TiltaksgjennomforingsTabell />
      </div>
    </div>
  );
};

export default ViewTiltakstypeOversikt;
