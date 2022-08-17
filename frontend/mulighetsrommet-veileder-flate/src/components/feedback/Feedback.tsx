import React, { useRef, useState } from 'react';
import { logEvent } from '../../api/logger';
import './Feedback.less';
import FeedbackButton from './FeedbackButton';
import FeedbackModal from './FeedbackModal';
import { Heading, useEventListener } from '@navikt/ds-react';
import Show from '../../utils/Show';

const Feedback = () => {
  const tilbakemeldingPrefix = 'har_sendt_tilbakemelding';

  const [isModalOpen, setModalOpen] = useState(false);
  const harSendtTilbakemelding = false;
  const wrapperRef = useRef<HTMLDivElement>(null);

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
    if (!isModalOpen) {
      setModalOpen(isModalOpen);
      logEvent('mulighetsrommet.tilbakemelding_modal_apnet');
    }
    setModalOpen(!isModalOpen);
  };

  return (
    <div ref={wrapperRef}>
      <FeedbackButton handleClick={handleClick} isModalOpen={isModalOpen} />
      <Show if={isModalOpen}>
        <FeedbackModal />
      </Show>
    </div>
  );
};

export default Feedback;
