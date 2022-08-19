import React, { useRef, useState } from 'react';
import { logEvent } from '../../core/api/logger';
import './Feedback.less';
import FeedbackButton from './FeedbackButton';
import FeedbackModalGrafana from './FeedbackModalGrafana';
import { useEventListener } from '@navikt/ds-react';
import Show from '../../utils/Show';
import FeedbackModalForms from './FeedbackModalForms';

const Feedback = () => {
  const [isModalOpen, setModalOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  //TODO når/hvis vi får godkjent grafana kan vi fjerne forms og fikse Grafana
  const grafana = false;

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
    <div ref={wrapperRef}>
      <FeedbackButton handleClick={handleClick} isModalOpen={isModalOpen} />
      <Show if={isModalOpen && grafana}>
        <FeedbackModalGrafana isModalOpen={isModalOpen} />
      </Show>

      <Show if={isModalOpen && !grafana}>
        <FeedbackModalForms isModalOpen={isModalOpen} />
      </Show>
    </div>
  );
};

export default Feedback;
