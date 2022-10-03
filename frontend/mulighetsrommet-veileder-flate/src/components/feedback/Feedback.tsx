import React, { useRef, useState } from 'react';
import { logEvent } from '../../core/api/logger';
import FeedbackButton from './FeedbackButton';
import { useEventListener } from '@navikt/ds-react';
import FeedbackModalForms from './FeedbackModalForms';
import { FEEDBACK, useFeatureToggles } from '../../core/api/feature-toggles';

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
        <div ref={wrapperRef}>
          <FeedbackButton handleClick={handleClick} isModalOpen={isModalOpen} />
          <FeedbackModalForms isModalOpen={isModalOpen} />
        </div>
      )}
    </>
  );
};

export default Feedback;
