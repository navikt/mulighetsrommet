import * as React from 'react';
import { useState } from 'react';
import { BodyShort } from '@navikt/ds-react';
import classNames from 'classnames';
import './FeedbackTilfredshet.less';

interface FeedbackTilfredshetValgProps {
  sporsmal: string;
  onTilfredshetChanged: (tilfredshet: number) => void;
}

function FeedbackTilfredshetValg({ sporsmal, onTilfredshetChanged }: FeedbackTilfredshetValgProps) {
  const [tilfredshet, setTilfredshet] = useState(0);

  const handleTilfredshetChanged = (tilfredshet: number) => {
    onTilfredshetChanged(tilfredshet);
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
          <img
            alt="Veldig lite tilfreds"
            src="data:img/png;base64,iVBORw0KGgoAAAANSUhEUgAAADQAAAA0CAMAAADypuvZAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAABZVBMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHBAQAAAAAAAAAAAAOCQgAAAAAAAAIBQQAAAAAAAACAQEAAAAAAAAWDg0AAAAAAAAAAABBKSZ4TEaZYlm3dWvGfnPQhXpQMy+SXlXNg3jWiX1oQjzLgnbKgXZVNjLBe3CqbWMuHRpNMS3PhHm5dmwmGBYeExGhZ16XYFgZEA43IyArGxkbERBHLSkDAgHBfHEFAwPUh3yub2XLgne7eG2+eW/Ae3AKBgXMg3fOhHgiFhRZOTRKLysxHxynamEoGRfEfXK9eW45JSEsHBmxcWeHV08kFxUVDQwNCAc7JiJwR0HTh3t2S0UQCglpQz1xSEJcOzU+JyQJBQU5JCHGf3Q4IyBUNjG8eG7QhXmOW1MlGBYBAQAnGRcRCwrViHwCAQFJLioKBgaycmhfPTeGVU6TXlZ+UUpGLSlOMi7RhnqmKjjhAAAAHXRSTlMAJWaVvN7s+S+M4jbA/r8kqv4E+v4Tvv4r3f5F8rmg0BYAAAABYktHRACIBR1IAAAACXBIWXMAAAsSAAALEgHS3X78AAAAB3RJTUUH4wIEDh0KvIGvswAAAltJREFUSMedVud/2kAMNRuTQtgEwmMZOAjDdA860pWEJB3pCt07HUn3+PtrmxR054CN3yfknx53J+lJkiQzXG6P1+cPBPw+r8ftkmwgKIfAISQHLSgLxzS3cKFYKlcUpVIuFQth7UNkYQZlMQrEqrU6I6jXqjEgujiFEpcTSDaazISVRhIJOX4UJ5VGptVmR6LdyiCdMnOWssh12FR0csguiZzlPLptNgPtLvLLwjl5qD02Ez0Vee6sVBYqs4SKLHlXPI1uz5rU6yI9iaGMXNuao70rB3mc00SmY4ejxTCT+J/lKFr2OIy1ED2sNyRtXc64YBKjOoygYZfDWAMRQwuI0Xo7fuLkKep1+gyX9JUYgkboqtTpLHCO2ueBC9SuGgEMoUb/SZNPn2jjomZfukwcaghp2kaY6ueKLtbViX1Vt68Rh3oYLsmNAj39uuZzg9g3+9qHNepRgFvyoMiFZx3YoPYA2OQcivBIXpS4b1vbhVvUvn3n7g7nUIJX8qFsP0s6yvBJflTmI1XglwJQ5iMpCDgjObqeo0CIIbeEHnIxuZbQkyuUkTX0MhIK1hJGwQrSMLB6T73/4OH6o90dM8mQhihCxoaPn2B74+mz5y9e4tWWSBqJUJA7e/2m//bd6Of7vQ8fG0OO0xzJXWgs9U+fSY/Y3z0Y7FPSYWMRWtiXr9/4Nxx8J9a4hXHN8gd+Co/4hb2JMW6WXFv+/ccUr8GkG5G2TAeAMjSRhn/HlyMDwNmocTTUnI1PZ4Pa2UrgbPmYseY0p685kqOFyqjD+Vc3YzDOvyQasFxH/wGJw0IAM0IucwAAAABJRU5ErkJggg=="
            // src="tilfredshet-ikoner/tilfredshet-1.png"
          />
        </button>

        <button className={hentKlasserForIkon(2)} onClick={() => handleTilfredshetChanged(2)}>
          <img
            alt="Lite tilfreds"
            src="data:img/png;base64,iVBORw0KGgoAAAANSUhEUgAAADQAAAA0CAMAAADypuvZAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAABU1BMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIBgMAAAAAAAAAAAAQDAYAAAAAAAAJBwMAAAAAAAACAgEAAAAAAAAaFAoAAAAAAAAAAABOOR+PaTm3h0nbolfsrl75uGNgRyavgUb1tWL/vWZ8WzHys2DxsmBmSyjmqlzLllE3KBZcRCT3t2Ldo1guIhIkGg7Ajky0hUgeFgxCMBo0JhQhGA1VPyIEAgHnq1wHBQL9u2XQmlPztGHgplnjqFrlqVsMCAT0tGEBAAD2tmIpHhBrTypZQSM7KxfHk08wIxPqrV3ip1pFMxs1JxXTnFTusF/DkE6YcDxxUy1hRyZSPCBEMhtKNh1nTCmAXjOrfkTWnlX7umSkeUEsIBE+LRhNOR5bQyRGMxw4KRYbFAoXEQn+vGXrrl6xg0bYoFb8umTwsWDFkk68i0tnEQ5UAAAAHXRSTlMAJWaVvN7s+S+M4jbA/r8kqv4E+v4Tvv4r3f5F8rmg0BYAAAABYktHRACIBR1IAAAACXBIWXMAAAsSAAALEgHS3X78AAAAB3RJTUUH4wIEDiIlT4641gAAAilJREFUSMedVmdj2kAMPTYmhbAJhGc2B8GM7jbpbpO2aUb33nu3//9T7aMh8hE4c+8TsvW4s6QnibFp+PyBYCgciYRDwYDfxzwgasTgQsyIKihLx2y3uFmp1uqNRr1WrZhx+0FiaQ5lOQmkmq02J2i3mikguTyDkjYyyHa6fAprnSwyRvooTi6PQs/iR8LqFZDPTXNWiij1+Uz0SyiuyJzVMgYWnwNrgPKqdE4ZwxGfi9EQZddZuSKGXIkhiuS70nkMRmrSaID8YQwNlCw1x/6uEoxJTjOFvheOHcNC5iDLSfS8cTjvIfm/3pD1dDlxwSzGdZhAxyuH8w4SQgtI0Xo7fuLkKep1+owr6WspREXomtTpLHCO2ueBdWo3RQBjaNF/suWzQbRxwbYvXiIOLcRsbSNO9XPZEeuVQ/uqY18jDu04fMwPk55+3fa5QezNDWDrJvUw4WcBVFzhuQXcpvY2cMflUEGABVF1PdvZNfeovX/33n2XQxVBFkLNe5Yc1BBiYdQXI9URZhE0FiM1ENEjaV1PKxByyJVwQi4nVwknuVIZqeGUkVSwSoiClaShhJCGLEIVxiKU5K5Adyx3rcYy3cIePHz0+MnTZ8+3X7x89fqN69WkhcnN8q3dFd69//Bx/dPnL8DXb/TdpFnKbfn7j5+/Dn7//vOXckhb1hoAeqNGa6jpjU+9Qa23EugtH3PWnO7sNYdpLVSiDhdf3cRgXHxJFFCuo/8ANC89uebOqpcAAAAASUVORK5CYII="
          />
        </button>

        <button className={hentKlasserForIkon(3)} onClick={() => handleTilfredshetChanged(3)}>
          <img
            alt="Helt ok tilfreds"
            src="data:img/png;base64,iVBORw0KGgoAAAANSUhEUgAAADQAAAA0CAMAAADypuvZAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAABI1BMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIBwUAAAAAAAAAAAAQDgoAAAAAAAAJCAYAAAAAAAACAgEAAAAAAAAaFhEAAAAAAAAAAABOQTGPeFu3mnTbuIvsxpb50Z9gUD2vk2/1zpz/16N8aE/yzJrxy5pmVkHmwZPLq4E3LiNcTTr30J3duo0uJh0kHhfAoXq0l3MeGRNCNyo0KyEhGxVVRzYEAwLnwpMHBQT91aHQr4TzzJvgvI/jv5HlwZIMCgf0zZsBAAD2z50pIhprWkRZSzg7MSXHp38wKB7qxZXivpBFOiw1LCHTsYajiWjow5Q8MiYNCghYSjh9aU+KdFiIclaLdVjwypn706Cqj2xfUDw+NCfNtWJZAAAAHXRSTlMAJWaVvN7s+S+M4jbA/r8kqv4E+v4Tvv4r3f5F8rmg0BYAAAABYktHRACIBR1IAAAACXBIWXMAAAsSAAALEgHS3X78AAAAB3RJTUUH4wIEDiEhY84vDAAAAhJJREFUSMedVnd/2jAQFRuTslcgPLMRBGPoTJukTUfSkXSlbfZov/+nqC3a5CyGjN5/5989S7q7d3eMzSIQDIUj0VgsGgmHggHmA3EjAQ8SRlxBWXvguCXNeqPZardbzUbdTDofUmtLKOkMkO10e5yg1+1kgUx6ASVn5FHoD/gMNvsF5I3cPE6xhPLQ4nNhDcsoFWc56xVUR3whRlVU1mXORg22xZfAslHbkM6pYTzhSzEZo+Y5q1jBmCsxRoW8K1eCPVGTJjZK9zE0ULXUHOddVRh3Oc2XR344TgzL+f9ZzmDoj8P5EJl/9YaCr8uJCxYwrcMU+n45nPeRElpAltbbw0ePn1Cvp888Sd/MIi5C16FOW8Bzar8AtqndEQFMoEv/5Mhnh2hj17FfviIOXSQcbSNJ9bPnivX1vf3Gtd8Sh14SARaESU9/5/jsE/tgB3j/gXqYCLIQ6p7wfAQ+UfsQOPI41BFiYTQ83z5/Mb9S+9v34x8ehwbCLIKm/yy5aCLComitRmohymJor0ZqI6ZH0rqeViDkkCvhhlxOrhJucqUyUsMtI6lglRAFK0lDCSENWYQqTEUoyV2BwVTuWo1Fr4XNaZY/fy0g3TXLOW355PTs/OLy6lrmkLY8OwBubn9vHdt/5KqkA0Bv1GgNNb3xqTeo9VYCveVjyZozWLzmMK2FStTh6qubGIyrL4kCynX0Lxl9ObixL1YSAAAAAElFTkSuQmCC"
            data-testid="tilfredshet-ikon_3"
          />
        </button>

        <button className={hentKlasserForIkon(4)} onClick={() => handleTilfredshetChanged(4)}>
          <img
            alt="Tilfreds"
            src="data:img/png;base64,iVBORw0KGgoAAAANSUhEUgAAADQAAAA0CAMAAADypuvZAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAABOFBMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFBwYAAAAAAAAAAAAKDQsAAAAAAAAAAAAAAAABAgEAAAAAAAAQFhIAAAAAAAAAAAAvPzVWdGJvlX6FspePwKKXy6s6TkJqjniUx6mb0LBLZVWTxaeSxKY+U0aLu557pYwhLCU3Sz+WyaqGtJgbJR8VHRh0nIRtknwSGBQoNS0fKiMUGhYzRToCAwKMvJ8EBQSZzq5+qY+TxqeItpqJuZyLup4HCQiUx6iVyKkYIRxBV0k2SD0jMCh4ookdJyGOvqGJuJspOC8gKySArJEwQTdIYVJjhHB8poyGtZmQwaOYzK2DsJVxmIFXdWM8UUWaz68wQDYVHBgBAQEgLCVFXE5/qpB1nYVskXpjhXFninVwln/KlKEkAAAAHHRSTlMAJWaVvN7s+S+M4jbA/r8kqv4E+hO+/ivd/kXycrj3ZwAAAAFiS0dEAIgFHUgAAAAJcEhZcwAACxIAAAsSAdLdfvwAAAAHdElNRQfjAgQOHzU40eAMAAACJElEQVRIx51WZ2PaQAw9NiY1YQfCM9sHwWDapjNt07TpTPde6V7//x/UNg3RHePMvW869LizpCeJsXlEorF4IplKJRPxWDTCQiBtZCAgY6QVlI0znptpNZqtdqfTbjUblukdZDdWUDZzQL7bszmB3evmgdzmEkrBKMLsD/gcdvomikZhEadURmXo8IVwhhWUS/OcrSpqI74UoxqqWzJnu46xw1fAGaO+Ld1ThzvhKzFxURfuKlXhciVcVMl3FcoYT9SkyRjl0xgaqDlqjvddNRiznBYrozAcL4aV4kmWcxiG43A+RO5/vcEM9bjggSamdZhFPyyH8z6ygRaQp/V29tz5Xep14aKQ9J080kHoutTpEnCZ2leAq9TuBgHMoEf/yZPPHtHGNc++foM49JDxtA2T6mffF+vNU/vAt28RB9tEhEVh0dtvez6HxL6z5x3cpR4WoiyGhhCee8B9aj8AHgoODcRYHE3h7OiR9ZjaT54+ey44NBFnCbTCZ8lHCwmWRHs9UhtJlkJnPVIHKT2S1vO0AiGHXAk/5HJylfCTK5WRGn4ZSQWrRFCwkjSUCKQhi1CFqQgluSswmMpdq7HMtbDdFy9fvX7z9p3tvP/w8ZN7TH+btTC5We5//vL1ZEoffvsuXDRrlgvasv3j56/ff/4eHRyL56Qtaw0AvVGjNdT0xqfeoNZbCfSWjxVrzmD5msO0FqqgDtdf3YLBuP6SGEC5jv4DDlszpKlLFewAAAAASUVORK5CYII="
            data-testid="tilfredshet-ikon_4"
          />
        </button>

        <button className={hentKlasserForIkon(5)} onClick={() => handleTilfredshetChanged(5)}>
          <img
            alt="Veldig tilfreds"
            src="data:img/png;base64,iVBORw0KGgoAAAANSUhEUgAAADQAAAA0CAMAAADypuvZAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAABQVBMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABBQMAAAAAAAAAAAADCgYAAAAAAAACBgMAAAAAAAAAAQEAAAAAAAAFEQoAAAAAAAAAAAARMR0fWjYoc0UwilMzlVk2nV4VPCQmbkI1ml04oWEbTi81mFw0mFsWQCYykVcsgE0MIhQUOiI2m10xjlUIGA4JHBEIGQ8TOCIbTy8GEQoQMB0BBQMSNR8JGxAcUjEiYzwEDAcyklguhVA0lloXQigvh1EviFIXQygKHhINJhc3n18PLRsKHREqekkAAgEYRiogXDcnckQgXjgeWDUqeUkBBAIYRyoUOyQ0lVojZT0NJxcgXjkDCgYtg08ncEMwjFQBAwEDCQUMIxUHFAwlakAjZj4ZRysSMx8QLhwTOSIbTS4re0o3n2CSKFGVAAAAHXRSTlMAJWaVvN7s+S+M4jbA/r8kqv4E+v4Tvv4r3f5F8rmg0BYAAAABYktHRACIBR1IAAAACXBIWXMAAAsSAAALEgHS3X78AAAAB3RJTUUH4wIEDg4gBtUn9wAAAjlJREFUSMedVmdj2jAUFBuTQtgEwrFBEMxqQhLIJB1p0126917//wfUCEolGbDRfTvxDktvE2KGw+lye7w+n9fjdjkdxAb8WgACAprfQrJxzTAL5vKFYqlcLhUL+VzQOAhtrJBshoFIpVqjHGrVSgQIby6RRLUY4vUGNWGnHkdMiy7SJJJINXW6EHozhWTCrNlKI9OiS9HKIL0la7azaOt0BfQ2stvSd7LodOlKdDvICt9KpNGhluggzb0rmkS7ay3qtpH870MNGd1aY7wrA20e01iqZUdj+DAV+xflMJr2NJQ2EZ7lG+K2LscuGMc0D0Oo29VQWkeI1QIifL71ru/2eKve7h7PdyLwM9dVeKM+sM/zfaDP8wpzYABV/vAAOOT5IXDA8yoCRm0jKNTPABjyfAgMeF4LwkGcyAkvPQKOeX4MHAkGOTiJC3nh7ASnZzw/O8WJYJCHi7hREJ16PhL56FzkBbiJB0WbIZqhCA/xorSeqAQv8aG8nqgMn5pI6XpKjjC53AoTl8vBtcQkuHIaWWKSRnLCXtyQrW7e4hlLWLk0bl/ekUR3r3jGSkMuwnv3H4iah3jE02kRSuVOH+MJT5+OhT9pTMvd1Fie4fmLOXk5fiVkzKyxmFvY6/Gb+lv26HeXGAr9et7CzM3yff8DPl592hvg8xfxl3mzXNSWR1+/ff/x89fvP+Ix15aVBoDaqFEaamrjU21Qq60EasvHijWnsXzNIUoLFcvD9Vc3NhjXXxIZLNfRv/8qPkEO8w3mAAAAAElFTkSuQmCC"
            data-testid="tilfredshet-ikon_5"
          />
        </button>
      </div>
    </>
  );
}

export default FeedbackTilfredshetValg;
