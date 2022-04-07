import React, { useEffect, useState } from 'react';
import { logEvent } from '../../api/logger';
import { Button, Tooltip } from '@navikt/ds-react';
import './Kopiknapp.less';

interface KopiknappProps {
  children: React.ReactNode;
  kopitekst: string;
}

const Kopiknapp = ({ children, kopitekst }: KopiknappProps) => {
  const [showTooltip, setShowTooltip] = useState(false);

  const copyToClipboard = (kopitekst: string, e: React.MouseEvent) => {
    e.stopPropagation();
    logEvent('mulighetsrommet.kopiknapp');
    navigator.clipboard.writeText(kopitekst);
    setShowTooltip(true);
  };

  useEffect(() => {
    let timeOutId = 0;
    if (showTooltip) {
      timeOutId = window.setTimeout(() => setShowTooltip(false), 1200);
    }
    return () => clearTimeout(timeOutId);
  }, [showTooltip]);

  return (
    <Tooltip placement="top" content="Kopiert" open={showTooltip}>
      <Button
        size="xsmall"
        variant="secondary"
        className="kopiknapp"
        onClick={e => {
          copyToClipboard(kopitekst, e);
        }}
      >
        {children}
      </Button>
    </Tooltip>
  );
};

export default Kopiknapp;
