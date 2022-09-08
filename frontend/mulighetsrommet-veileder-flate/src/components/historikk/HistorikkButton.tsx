import { Button } from '@navikt/ds-react';
import { useState } from 'react';
import { useFeatureToggles, VIS_HISTORIKK } from '../../core/api/feature-toggles';
import { useHentHistorikk } from '../../core/api/queries/useHentHistorikk';
import StandardModal from '../modal/StandardModal';
import { HistorikkForBruker } from './HistorikkForBruker';
import './HistorikkForBruker.less';

export function HistorikkButton() {
  const [apneModal, setApneModal] = useState(false);
  const features = useFeatureToggles();
  const toggleModal = () => setApneModal(!apneModal);
  const visHistorikkKnapp = features.isSuccess && features.data[VIS_HISTORIKK];
  const { isLoading } = useHentHistorikk(visHistorikkKnapp);

  if (isLoading || !visHistorikkKnapp) return null;

  return (
    <>
      <Button onClick={toggleModal}>Historikk</Button>
      <StandardModal
        className="historikk-modal"
        hideButtons
        modalOpen={apneModal}
        setModalOpen={toggleModal}
        heading="Aktivitet"
        handleForm={() => {}}
      >
        <HistorikkForBruker />
      </StandardModal>
    </>
  );
}
