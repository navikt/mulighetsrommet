import { useEventListener } from '@navikt/ds-react';
import { useRef, useState } from 'react';
import { FEEDBACK, useFeatureToggles } from '../../core/api/feature-toggles';
import { logEvent } from '../../core/api/logger';
import FeedbackButton from './FeedbackButton';
import FeedbackModalForms from './FeedbackModalForms';

const Feedback = () => {
  const [isModalOpen, setModalOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const features = useFeatureToggles();
  const visFeedback = features.isSuccess && features.data[FEEDBACK];

  const handleClickOutside = (e: { target: Node | null }) => {
    if (wrapperRef.current?.contains(e.target)) {
      return;
    }
    if (isModalOpen) {
      setModalOpen(false);
    }
  };

  useEventListener('mousedown', handleClickOutside);

  const handleClick = () => {
    setModalOpen(!isModalOpen);
    !isModalOpen && logEvent('mulighetsrommet.tilbakemelding_modal_apnet');
  };

  return (
    <>
      {visFeedback && (
        <div ref={wrapperRef} style={{ marginTop: '2rem' }}>
          <FeedbackButton handleClick={handleClick} isModalOpen={isModalOpen} />
          <FeedbackModalForms isModalOpen={isModalOpen} />
        </div>
      )}
    </>
  );
};

export default Feedback;
