import * as React from 'react';
import { useState } from 'react';
import { BodyShort } from '@navikt/ds-react';
import './FeedbackTilfredshet.less';
import tilfredshet1 from './tilfredshet-1.png';
import tilfredshet2 from './tilfredshet-2.png';
import tilfredshet3 from './tilfredshet-3.png';
import tilfredshet4 from './tilfredshet-4.png';
import tilfredshet5 from './tilfredshet-5.png';
import classNames from 'classnames';
import { useFeedbackTilfredshet } from '../../../hooks/useFeedbackTilfredshet';

interface FeedbackTilfredshetValgProps {
  sporsmal: string;
}

const FeedbackTilfredshetValg = ({ sporsmal }: FeedbackTilfredshetValgProps) => {
  const [tilfredshet, setTilfredshet] = useState(0);

  const feedbackTilfredshet = useFeedbackTilfredshet(tilfredshet);

  const handleTilfredshetChanged = (tilfredshet: number) => {
    setTilfredshet(tilfredshet);
  };

  const hentKlasserForIkon = (ikonTilfredshet: number): any => {
    const erValgt = ikonTilfredshet === tilfredshet;
    const harValgt = tilfredshet > 0;
    return classNames('feedback__tilfredshet--btn', {
      'feedback__tilfredshet--btn--valgt': erValgt,
      'feedback__tilfredshet--btn--ikke-valgt': harValgt && !erValgt,
    });
  };

  return (
    <>
      <BodyShort>
        <strong>{sporsmal}</strong>
      </BodyShort>

      <div className="feedback__tilfredshet">
        <button className={hentKlasserForIkon(1)} onClick={() => handleTilfredshetChanged(1)}>
          <img alt="Veldig lite tilfreds" src={tilfredshet1} />
        </button>

        <button className={hentKlasserForIkon(2)} onClick={() => handleTilfredshetChanged(2)}>
          <img alt="Lite tilfreds" src={tilfredshet2} />
        </button>

        <button className={hentKlasserForIkon(3)} onClick={() => handleTilfredshetChanged(3)}>
          <img alt="Helt ok tilfreds" src={tilfredshet3} />
        </button>

        <button className={hentKlasserForIkon(4)} onClick={() => handleTilfredshetChanged(4)}>
          <img alt="Tilfreds" src={tilfredshet4} />
        </button>

        <button className={hentKlasserForIkon(5)} onClick={() => handleTilfredshetChanged(5)}>
          <img alt="Veldig tilfreds" src={tilfredshet5} />
        </button>
      </div>
    </>
  );
};

export default FeedbackTilfredshetValg;
